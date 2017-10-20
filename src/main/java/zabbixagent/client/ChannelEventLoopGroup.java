package zabbixagent.client;


import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

public class ChannelEventLoopGroup {
    private EventLoopGroup eventLoopGroup;
    private Class<? extends Channel> channel;

    public ChannelEventLoopGroup(EventLoopGroup eventLoopGroup, Class<? extends Channel> channel) {
        this.eventLoopGroup = eventLoopGroup;
        this.channel = channel;
    }

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public Class<? extends Channel> getChannel() {
        return channel;
    }
}
