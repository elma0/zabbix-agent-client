package tests;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import zabbixagent.client.ZabbixAgentClient;
import zabbixagent.client.ZabbixAgentClient.Callback;
import zabbixagent.client.request.Metric;
import zabbixagent.client.request.Target;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static com.google.common.collect.Lists.newArrayList;
import static java.net.InetAddress.getLocalHost;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static tests.EchoServerHandler.VALUE;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-zabbix-agent-client.xml"})
public class ZabbixMetricProcessorTest {
    private Metric[] metrics;
    @Inject
    private ObjectMapper objectMapper;
    @Inject
    @Named("jsonFile")
    private File metricsFile;
    @Inject
    private ZabbixAgentClient connector;
    @Inject
    private RequestChecker requestChecker;
    @Inject
    private EchoServer server;

    @Before
    public void setUp() throws Exception {
        metrics = objectMapper.readValue(metricsFile, Metric[].class);
    }

    @Test
    public void testZabbixAgentSmallBacklog() throws Exception {
        int port = 10053;
        server.start(new String[]{"1", String.valueOf(port)});
        final LongAdder counter = new LongAdder();
        final CountDownLatch latch = new CountDownLatch(metrics.length);
        connector.fetch(new Target(getLocalHost().getHostAddress(), port), newArrayList(metrics), 100,
                new Callback() {
                    @Override
                    public void onEvent(Target target, Metric metric, String value) {
                        assertTrue(VALUE.equals(value));
                        latch.countDown();
                    }

                    @Override
                    public void onError(Target target, Metric metric, Throwable e) {
                        counter.increment();
                        latch.countDown();
                    }
                });
        latch.await(10000, TimeUnit.MILLISECONDS);
        System.out.println("Errors cnt: " + counter.sum());
        assertTrue(requestChecker.isAllRequestsOK());
        server.stop();
    }

    @Test
    public void testZabbixAgent() throws Exception {
        int port = 10051;
        server.start(new String[]{"100", String.valueOf(port)});
        LongAdder counter = new LongAdder();
        final CountDownLatch latch = new CountDownLatch(metrics.length);
        connector.fetch(new Target(getLocalHost().getHostAddress(), port),
                newArrayList(metrics),
                100,
                new Callback() {
                    @Override
                    public void onEvent(Target target, Metric metric, String value) {
                        assertTrue(VALUE.equals(value));
                        latch.countDown();
                    }

                    @Override
                    public void onError(Target target, Metric metric, Throwable e) {
                        latch.countDown();
                        counter.increment();
                    }
                });
        latch.await(10000, TimeUnit.MILLISECONDS);
        assertEquals(latch.getCount(), 0);
        assertEquals(0, counter.sum());
        assertTrue(requestChecker.isAllRequestsOK());
        server.stop();
    }

    @Test
    public void testZabbixAgentDiscard() throws Exception {
        final CountDownLatch latch = new CountDownLatch(metrics.length);
        LongAdder counter = new LongAdder();
        connector.fetch(new Target(getLocalHost().getHostAddress(), 10052),
                newArrayList(metrics),
                100,
                new Callback() {
                    @Override
                    public void onEvent(Target target, Metric metric, String value) {
                        counter.increment();
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
        assertEquals(0, counter.sum());
        assertTrue(requestChecker.isAllRequestsOK());
    }
}