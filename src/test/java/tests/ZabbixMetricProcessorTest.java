package tests;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
import java.util.concurrent.atomic.LongAdder;

import static com.google.common.collect.Lists.newArrayList;
import static java.net.InetAddress.getLocalHost;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static tests.EchoServerHandler.VALUE;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-zabbix-agent-client.xml"})
public class ZabbixMetricProcessorTest implements ApplicationContextAware {
    private Metric[] metrics;
    @Inject
    private ObjectMapper objectMapper;
    @Inject
    @Named("jsonFile")
    private File metricsFile;
    @Inject
    private EchoServer server;
    private static ZabbixAgentClient zabbixAgentClient;

    @Before
    public void setUp() throws Exception {
        metrics = objectMapper.readValue(metricsFile, Metric[].class);
    }

    @Test
    public void testZabbixAgentSmallBacklog() throws Exception {
        int port = 10053;
        server.start(1, port);
        final LongAdder counter = new LongAdder();
        final CountDownLatch latch = new CountDownLatch(metrics.length);
        zabbixAgentClient.fetch(new Target(getLocalHost().getHostAddress(), port), newArrayList(metrics), 100,
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
        latch.await(10, TimeUnit.SECONDS);
        assertTrue(counter.sum() < metrics.length);
        server.stop();
    }

    @Test
    public void testZabbixAgent() throws Exception {
        int port = 10051;
        server.start(100, port);
        LongAdder counter = new LongAdder();
        final CountDownLatch latch = new CountDownLatch(metrics.length);
        zabbixAgentClient.fetch(new Target(getLocalHost().getHostAddress(), port), newArrayList(metrics), 100,
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
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
        assertEquals(0, counter.sum());
        server.stop();
    }

    @Test
    public void testZabbixAgentDiscard() throws Exception {
        final CountDownLatch latch = new CountDownLatch(metrics.length);
        LongAdder counter = new LongAdder();
        zabbixAgentClient.fetch(new Target(getLocalHost().getHostAddress(), 10052), newArrayList(metrics), 100,
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
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
        assertEquals(0, counter.sum());
    }

    @AfterClass
    public static void stop(){
        zabbixAgentClient.shootdown();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        zabbixAgentClient = applicationContext.getAutowireCapableBeanFactory().getBean(ZabbixAgentClient.class);
    }
}