package zabbixagent.client.response;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import zabbixagent.client.ChannelValuePair;
import zabbixagent.client.ZabbixValue;

import javax.inject.Inject;

import static zabbixagent.client.ChannelFactoryImpl.CHANNEL_KEY;

@ChannelHandler.Sharable
public class MessageToEventPipe extends ChannelInboundHandlerAdapter {
    @Inject
    private ApplicationEventPublisher publisher;
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageToEventPipe.class);

    public MessageToEventPipe() {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            publisher.publishEvent(new ZabbixValue(new ChannelValuePair(ctx.channel().attr(CHANNEL_KEY).get(), ((ByteBuf) msg).toString(CharsetUtil.UTF_8))));
            ctx.close();
        } finally {
            ((ByteBuf) msg).release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Message receiving from zabbix agent failed", cause);
        publisher.publishEvent(new ZabbixValue(new ChannelValuePair(ctx.channel().attr(CHANNEL_KEY).get(), cause)));
        ctx.close();
    }
}