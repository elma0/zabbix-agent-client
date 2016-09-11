package zabbixagent.client.response;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import javax.inject.Inject;


public class ZbxChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Inject
    private MessageToEventPipe messageToEventPipe;

    public ZbxChannelInitializer() {
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new ZabbixHeaderDecoder(), messageToEventPipe);
    }
}
