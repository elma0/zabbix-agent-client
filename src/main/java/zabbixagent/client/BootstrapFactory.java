package zabbixagent.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.FactoryBean;

import javax.inject.Inject;

public class BootstrapFactory implements FactoryBean<Bootstrap> {
    @Inject
    private ChannelInitializer channelInitializer;
    @Inject
    private ChannelEventLoopGroup eventLoopGroup;

    @Override
    public Bootstrap getObject() throws Exception {
        return new Bootstrap()
                .group(eventLoopGroup.getEventLoopGroup())
                .channel(eventLoopGroup.getChannel())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(channelInitializer);
    }

    @Override
    public Class<?> getObjectType() {
        return Bootstrap.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}