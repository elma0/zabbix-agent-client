package tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import zabbixagent.client.request.Metric;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Handler implementation for the echo server.
 */
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter implements ApplicationContextAware {
    private Collection<String> keys;
    @Inject
    private ObjectMapper objectMapper;
    @Inject
    @Named("jsonFile")
    private File metricsFile;
    public static final String VALUE = "1";

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            Metric[] metrics = objectMapper.readValue(metricsFile, Metric[].class);
            keys = Stream.of(metrics).filter(metric -> !isEmpty(metric.getKey())).map(Metric::getKey).collect(toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String request = ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
        if (!keys.contains(request.substring(0, request.length() - 1))) {
            ctx.close();
        }
        ctx.write(
                wrappedBuffer(
                        new byte[]{90, 66, 88, 68, 1, 1, 0, 0, 0, 0, 0, 0, 0},
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