package tests;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zabbixagent.client.ZabbixAgentClient;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static java.net.InetAddress.getLocalHost;
import static tests.EchoServer.VALUE;

public class ZabbixMetricProcessorTest {
    private static Set<String> metrics;
    private static EchoServer server;
    private static ZabbixAgentClient zabbixAgentClient;
    private static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);

    @BeforeClass
    public static void setUp() throws Exception {
        metrics = Sets.newHashSet(new ObjectMapper().readValue(
                new File(ClassLoader.getSystemResource("metrics.json").toURI()),
                String[].class
        ));
        zabbixAgentClient = new ZabbixAgentClient(eventLoopGroup);
        server = new EchoServer(metrics);
    }

    @Test
    public void testZabbixAgentSmallBacklog() throws Exception {
        int port = 10053;
        InetSocketAddress address = server.start(1, port);
        zabbixAgentClient.fetch(address.getHostName(), port, Sets.newHashSet(metrics))
                .whenComplete((m, t) -> Assert.assertEquals(metrics.size(), m.size()))
                .whenComplete((m, t) -> m.values().forEach(v -> Assert.assertEquals(VALUE, v)))
        .join();
        server.stop();
    }

    @Test
    public void testZabbixAgent() throws Exception {
        int port = 10051;
        InetSocketAddress address = server.start(100, port);
        zabbixAgentClient.fetch(address.getHostName(), port, Sets.newHashSet(metrics), 100)
                .whenComplete((m, t) -> Assert.assertEquals(metrics.size(), m.size()))
                .whenComplete((m, t) -> m.values().forEach(v -> Assert.assertEquals(VALUE, v)))
                .join();
        server.stop();
    }

    @Test(expected = CompletionException.class)
    public void testZabbixAgentDiscard() throws Exception {
        zabbixAgentClient.fetch(getLocalHost().getHostAddress(), 10052, Sets.newHashSet(metrics), 100)
                .whenComplete((m, t) -> Assert.assertEquals(metrics.size(), 0))
                .join();
    }

    @AfterClass
    public static void stop() {
        eventLoopGroup.shutdownGracefully(5, 5, TimeUnit.SECONDS);
    }
}