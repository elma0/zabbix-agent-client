package tests;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public final class EchoServer {
    static final int PORT = Integer.parseInt(System.getProperty("port", "10050"));
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    private ChannelHandler handler;

    public EchoServer(ChannelHandler handler) {
        this.handler = handler;
    }

    public static void main(String[] args) throws Exception {
        EchoServer server = new EchoServer(new EchoServerHandler());
        server.start(new String[]{"3", "10050"});
    }

    public void start(String[] args) throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, Integer.parseInt(args[0]))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(handler);
                    }
                });
        b.bind(Integer.parseInt(args[1])).sync();
    }

    public void stop() throws InterruptedException {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}