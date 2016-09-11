package tests;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import zabbixagent.client.ZabbixAgentClient;
import zabbixagent.client.request.Metric;
import zabbixagent.client.request.Target;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.net.InetAddress.getLocalHost;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static tests.EchoServerHandler.VALUE;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-zabbix-agent-client.xml"})
public class ZabbixMetricProcessorTest implements ApplicationContextAware {
    public ZabbixMetricProcessorTest() {

    }

    private ApplicationContext applicationContext;
    private List<Metric> metrics;
    @Inject
    private ObjectMapper objectMapper;
    @Inject
    @Named("jsonFile")
    private File metricsFile;
    @Inject
    private ZabbixAgentClient connector;
    @Inject
    private RequestChecker requestChecker;

    @Before
    public void setUp() throws Exception {
        metrics = objectMapper.readValue(metricsFile, new TypeReference<List<Metric>>() {
        });
    }

    @Test
    public void testZabbixAgentSmallBacklog() throws Exception {
        EchoServer server = applicationContext.getBean(EchoServer.class);
        int port = 10053;
        server.start(new String[]{"1", String.valueOf(port)});
        final AtomicInteger counter = new AtomicInteger(0);
        int factor = 1;
        final CountDownLatch latch = new CountDownLatch(metrics.size() * factor);
        for (int i = 0; i < factor; i++) {
            connector.fetch(new Target(getLocalHost().getHostAddress(), port), metrics, 100,
                    new ZabbixAgentClient.Callback() {
                        @Override
                        public void onEvent(Target target, Metric metric, String value) {
                            assertTrue(VALUE.equals(value));
                            latch.countDown();
                        }

                        @Override
                        public void onError(Target target, Metric metric, Throwable e) {
                            counter.incrementAndGet();
                            latch.countDown();
                        }
                    });
        }
        latch.await(10000, TimeUnit.MILLISECONDS);
        System.out.println("Errors cnt: " + counter.get());
        System.out.println("Factor : " + factor);
        assertTrue(requestChecker.isAllRequestsOK());
        server.stop();
    }

    @Test
    public void testZabbixAgent() throws Exception {
        EchoServer server = applicationContext.getBean(EchoServer.class);
        server.start(new String[]{"100", "10051"});
        final int[] counter = {0};
        final CountDownLatch latch = new CountDownLatch(metrics.size());
        connector.fetch(new Target(getLocalHost().getHostAddress(), 10051), metrics, 100, new ZabbixAgentClient.Callback() {
            @Override
            public void onEvent(Target target, Metric metric, String value) {
                assertTrue(VALUE.equals(value));
                latch.countDown();
            }

            @Override
            public void onError(Target target, Metric metric, Throwable e) {
                latch.countDown();
                counter[0]++;
            }
        });
        latch.await(10000, TimeUnit.MILLISECONDS);
        assertEquals(latch.getCount(), 0);
        assertEquals(counter[0], 0);
        assertTrue(requestChecker.isAllRequestsOK());
        server.stop();
    }

    @Test
    public void testZabbixAgentDiscard() throws Exception {
        final CountDownLatch latch = new CountDownLatch(metrics.size());
        final int[] counter = {0};
        connector.fetch(new Target(getLocalHost().getHostAddress(), 10052), metrics, 100, new ZabbixAgentClient.Callback() {
            @Override
            public void onEvent(Target target, Metric metric, String value) {
                counter[0]++;
                latch.countDown();
            }

            @Override
            public void onError(Target target, Metric pair, Throwable e) {
                System.out.printf(e.toString());
                latch.countDown();
            }

        });
        latch.await(10000, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
        assertEquals(0, counter[0]);
        assertTrue(requestChecker.isAllRequestsOK());
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}