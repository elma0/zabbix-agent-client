package zabbixagent.client;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.FactoryBean;

import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.valueOf;

public class EventLoopGroupFactory implements FactoryBean<ChannelEventLoopGroup> {
    private Integer workerThreads = parseInt(System.getProperty("za.client.worker.threads",
            valueOf(getRuntime().availableProcessors() * 2)));

    private ChannelEventLoopGroup newEventLoopGroup(int ioThreadCount) {
        if (Epoll.isAvailable()) {
            return newEpollEventLoopGroup(ioThreadCount);
        }
        return newNioEventLoopGroup(ioThreadCount);
    }

    private ChannelEventLoopGroup newNioEventLoopGroup(int ioThreadCount) {
        if (ioThreadCount > 0) {
            return new ChannelEventLoopGroup(new NioEventLoopGroup(ioThreadCount), NioSocketChannel.class);
        } else {
            return new ChannelEventLoopGroup(new NioEventLoopGroup(), NioSocketChannel.class);
        }
    }

    private ChannelEventLoopGroup newEpollEventLoopGroup(int ioThreadCount) {
        if (ioThreadCount > 0) {
            return new ChannelEventLoopGroup(new EpollEventLoopGroup(ioThreadCount), EpollSocketChannel.class);
        } else {
            return new ChannelEventLoopGroup(new EpollEventLoopGroup(), EpollSocketChannel.class);
        }
    }

    @Override
    public ChannelEventLoopGroup getObject() throws Exception {
        return newEventLoopGroup(workerThreads);
    }

    @Override
    public Class<?> getObjectType() {
        return ChannelEventLoopGroup.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
