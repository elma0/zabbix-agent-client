package zabbixagent.client;

import com.google.common.collect.Multimap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import zabbixagent.client.request.Metric;
import zabbixagent.client.request.Target;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;


public class ZabbixAgentClient {
    @Inject
    @Named("metrics")
    private Map<String, Pair<Target, Metric>> keymap;
    @Inject
    @Named("callbacks")
    private Map<String, Callback> callbacks;
    @Inject
    @Named("channelFactory")
    private ChannelFactory channelFactory;
    private static final Logger LOGGER = LoggerFactory.getLogger(ZabbixAgentClient.class);

    @EventListener
    private void onApplicationEvent(ZabbixValue zabbixValue) {
        ChannelValuePair pair = ((ChannelValuePair) zabbixValue.getSource());
        String channel = ((ChannelValuePair) zabbixValue.getSource()).getChannel();
        Callback callback = callbacks.remove(channel);
        Pair<Target, Metric> metric = keymap.remove(channel);
        if (pair.getThrowable() == null) {
            callback.onEvent(metric.getKey(), metric.getValue(), ((ChannelValuePair) zabbixValue.getSource()).getValue());
        } else {
            callback.onError(metric.getKey(), metric.getValue(), ((ChannelValuePair) zabbixValue.getSource()).getThrowable());
        }
    }


    public interface Callback {
        void onEvent(Target target, Metric metric, String value);

        void onError(Target target, Metric metric, Throwable e);
    }

    public void fetch(Multimap<Target, Metric> request, int soBacklog, final Callback callback) {
        for (Map.Entry<Target, Collection<Metric>> entry : request.asMap().entrySet()) {
            fetch(entry.getKey(), entry.getValue(), soBacklog, callback);
        }
    }


    public Map<Metric, String> fetch(final Target object, Collection<? extends Metric> mkeys, int soBacklog, int timeout) throws InterruptedException {
        final Map<Metric, String> result = new HashMap<>();
        final CountDownLatch latch = new CountDownLatch(mkeys.size());
        fetch(object, mkeys, soBacklog, new Callback() {
            @Override
            public void onEvent(Target target, Metric metric, String value) {
                result.put(metric, value);
                latch.countDown();
            }

            @Override
            public void onError(Target target, Metric metric, Throwable e) {
                latch.countDown();
                LOGGER.error("Error during metric {} gathering from {} ", metric, target.getHostname(), e);
            }
        });
        latch.await(timeout, TimeUnit.MILLISECONDS);
        return result;
    }

    public void fetch(final Target object, Collection<? extends Metric> mkeys, int soBacklog, final Callback callback) {
        final Queue<Metric> metricsQueue = new ConcurrentLinkedQueue<>(mkeys);
        for (int i = 0; i < getConcurrentConnectionsCount(soBacklog, mkeys.size()); i++) {
            pollNextMetric(object, metricsQueue, callback);
        }
    }

    private static class ZabbixChannelListener implements ChannelFutureListener {
        private Target object;
        private Queue<Metric> metricsQueue;
        private Callback callback;
        private ZabbixAgentClient connector;

        ZabbixChannelListener(ZabbixAgentClient connector, Target object, Queue<Metric> metricsQueue, Callback callback) {
            this.object = object;
            this.metricsQueue = metricsQueue;
            this.callback = callback;
            this.connector = connector;
        }

        @Override
        public void operationComplete(ChannelFuture channelFuture) throws Exception {
            if (channelFuture.isSuccess()) {
                connector.pollNextMetric(object, metricsQueue, callback);
            }
        }
    }

    private void pollNextMetric(Target object, Queue<Metric> metricsQueue, Callback callback) {
        Metric metric = metricsQueue.poll();
        if (metric != null) {
            channelFactory.requestMetric(object, metric, callback).channel().
                    closeFuture().addListener(new ZabbixChannelListener(this, object, metricsQueue, callback));
        }
    }

    private int getConcurrentConnectionsCount(int soBacklog, int metricsCount) {
        return soBacklog < metricsCount ? soBacklog : metricsCount;
    }
}