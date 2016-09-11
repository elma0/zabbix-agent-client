package zabbixagent.client;


import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import zabbixagent.client.request.Metric;
import zabbixagent.client.request.Target;

public interface ChannelFactory {
    AttributeKey<String> CHANNEL_KEY = AttributeKey.valueOf("za_id_");
    int DEFAULT_AGENT_PORT = 10050;

    ChannelFuture requestMetric(final Target target, Metric metric, ZabbixAgentClient.Callback callback);
}
