package zabbixagent.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.FactoryBean;

import javax.inject.Inject;

public class BootstrapFactory implements FactoryBean<Bootstrap> {
    @Inject
    private ChannelInitializer channelInitializer;

    public BootstrapFactory() {

    }

    @Override
    public Bootstrap getObject() throws Exception {
        Bootstrap bootstrap;
        EventLoopGroup workerGroup;
        workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, false);
        bootstrap.handler(channelInitializer);
        return bootstrap;
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