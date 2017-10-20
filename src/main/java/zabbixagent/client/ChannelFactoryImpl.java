package zabbixagent.client;


import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import zabbixagent.client.request.Metric;
import zabbixagent.client.request.Target;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.apache.commons.lang3.tuple.Pair.of;
import static zabbixagent.client.ZabbixAgentClient.Callback;

public class ChannelFactoryImpl implements ChannelFactory {

    @Inject
    private ApplicationEventPublisher publisher;
    @Inject
    @Named("bootstrap")
    private Bootstrap bootstrap;
    @Inject
    @Named("metrics")
    private Map<String, Pair<Target, Metric>> metricsMap;
    @Inject
    @Named("callbacks")
    private Map<String, Callback> callbacks;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelFactoryImpl.class);

    public ChannelFactoryImpl() {
    }

    public ChannelFuture requestMetric(Target target, Metric metric, final Callback callback) throws IllegalArgumentException{
        ChannelFuture channelFuture = null;
        final String metricKey = metric.getKey();
        if (metricKey != null) {
            channelFuture = bootstrap.connect(target.getHostname(), target.getPort());
            channelFuture.addListener((ChannelFuture future) -> {
                callbacks.put(future.channel().toString(), callback);
                future.channel().attr(CHANNEL_KEY).set(future.channel().toString());
                metricsMap.put(future.channel().toString(), of(target, metric));
                if (future.isSuccess()) {
                    ByteBuf request = wrappedBuffer((metricKey + "\n").getBytes());
                    future.channel().writeAndFlush(request);
                    LOGGER.trace("{} key = {}", target.getHostname(), metricKey);
                } else {
                    publisher.publishEvent(new ZabbixValue(new ChannelValuePair(future.channel().toString(), future.cause())));
                    future.channel().close();
                    LOGGER.error("Connection {}:{} failed", target.getHostname(), target.getPort());
                }
            });
        }
        return channelFuture;
    }
}