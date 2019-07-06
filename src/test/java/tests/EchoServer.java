package tests;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Set;

import static io.netty.buffer.Unpooled.wrappedBuffer;

final class EchoServer {
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private ChannelHandler handler;
    private static final byte[] HEADER = new byte[]{90, 66, 88, 68, 1, 1, 0, 0, 0, 0, 0, 0, 0};
    static final String VALUE = "1";

    EchoServer(Set<String> keys) {
        this.handler = new EchoServerHandler(keys);
    }

    @ChannelHandler.Sharable
    private static class EchoServerHandler extends ChannelInboundHandlerAdapter {
        private final Collection<String> keys;

        EchoServerHandler(Set<String> keys) {
            this.keys = keys;
        }

        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            String request = ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
            if (!keys.contains(request.substring(0, request.length() - 1))) {
                ctx.close();
            }
            ctx.write(
                    wrappedBuffer(
                            HEADER,
                            VALUE.getBytes())
            ).addListener((ChannelFuture channelFuture) -> {
                if (channelFuture.isSuccess()) {
                    channelFuture.channel().close();
                }
            });
        }


        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            super.channelReadComplete(ctx);
            ctx.channel().flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    InetSocketAddress start(int soBacklog, int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, soBacklog)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(handler);
                    }
                });
        return (InetSocketAddress) b.bind(port).syncUninterruptibly().channel().localAddress();
    }

    void stop() throws InterruptedException {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully().sync();
    }
}