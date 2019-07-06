package zabbixagent.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Charsets.UTF_8;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static java.lang.Byte.SIZE;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;


public class ZabbixAgentClient {
    private static final AttributeKey<String> CHANNEL_KEY = AttributeKey.valueOf("zbx_key");
    private static final int BATCH_SIZE = 5;
    private final Map<String, CompletableFuture<Map.Entry<String, String>>> map = new HashMap<>();
    private final Bootstrap bootstrap;

    public ZabbixAgentClient(EventLoopGroup eventLoopGroup) {
        this.bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new ZabbixHeaderDecoder());
                        pipeline.addLast(new Handler(map));
                    }
                });
    }

    private static class ZabbixHeaderDecoder extends ReplayingDecoder<ZabbixHeaderDecoder.DecodingState> {
        public enum DecodingState {
            HEADER,
            VERSION,
            DATA_LENGTH,
            DATA,
        }

        private Long dataLength;
        private static final byte[] ZBX_HEADER = "ZBXD".getBytes();
        private static final byte[] HEADER_VERSION = "\001".getBytes();
        private static final int DATA_LENGTH = 8;
        private static final Logger LOGGER = LoggerFactory.getLogger(ZabbixHeaderDecoder.class);

        ZabbixHeaderDecoder() {
            super(DecodingState.HEADER);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            switch (state()) {
                case HEADER:
                    int head = in.readInt();
                    byte[] header = allocate(Integer.SIZE / SIZE).putInt(head).array();
                    if (!Arrays.equals(header, ZBX_HEADER)) {
                        LOGGER.error("Incorrect Header");
                        break;
                    }
                    checkpoint(DecodingState.VERSION);
                case VERSION:
                    byte ver = in.readByte();
                    byte[] version = {ver};
                    if (!Arrays.equals(version, HEADER_VERSION)) {
                        LOGGER.error("Incorrect Version");
                        break;
                    }
                    checkpoint(DecodingState.DATA_LENGTH);
                case DATA_LENGTH:
                    long length = in.readLong();
                    ByteBuffer dataLengthBuf = allocate(DATA_LENGTH).order(LITTLE_ENDIAN).putLong(length);
                    dataLengthBuf.order(BIG_ENDIAN);
                    dataLengthBuf.position(0);
                    dataLength = dataLengthBuf.getLong();
                    checkpoint(DecodingState.DATA);
                case DATA:
                    ByteBuf frame = in.readBytes(dataLength.intValue());
                    checkpoint(DecodingState.HEADER);
                    out.add(frame.toString(UTF_8));
                    break;
                default:
                    throw new IllegalStateException("Incorrect State");
            }
        }
    }

    private static class Handler extends SimpleChannelInboundHandler<String> {
        private final Map<String, CompletableFuture<Map.Entry<String, String>>> map;

        Handler(Map<String, CompletableFuture<Map.Entry<String, String>>> map) {
            this.map = map;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String s) {
            String key = ctx.channel().attr(CHANNEL_KEY).get();
            map.remove(key).complete(new AbstractMap.SimpleEntry<>(key, s));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            map.remove(ctx.channel().attr(CHANNEL_KEY).get()).completeExceptionally(cause);
            ctx.close();
        }
    }

    public CompletableFuture<Map<String, String>> fetch(
            final String host, int port, Set<String> mkeys
    ) throws InterruptedException {
        return fetch(host, port, mkeys, BATCH_SIZE);
    }

    public CompletableFuture<Map<String, String>> fetch(
            final String host, int port, Set<String> mkeys, int batch
    ) throws InterruptedException {
        final BlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(mkeys.size(), false, mkeys);
        final BlockingQueue<String> queue = new ArrayBlockingQueue<>(batch);
        inputQueue.drainTo(queue, batch);
        List<CompletableFuture<Map.Entry<String, String>>> list = new ArrayList<>();
        while (!queue.isEmpty() || !inputQueue.isEmpty()) {
            String metric = queue.poll(5, TimeUnit.MINUTES);
            list.add(pollNextMetric(host, port, metric)
                    .whenComplete((s, t) -> inputQueue.drainTo(queue, 1)));
        }
        return allOf(list);
    }

    private CompletableFuture<Map<String, String>> allOf(List<CompletableFuture<Map.Entry<String, String>>> list) {
        CompletableFuture<Void> none = CompletableFuture.allOf(list.toArray(new CompletableFuture[0]));
        return none.thenApply(voit -> list.stream().map(CompletableFuture::join)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    private CompletableFuture<Map.Entry<String, String>> pollNextMetric(String host, int port, String metric) {
        if (metric == null) {
            throw new IllegalArgumentException();
        }
        CompletableFuture<Map.Entry<String, String>> completableFuture = new CompletableFuture<>();
        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.addListener(c -> {
            if (c.cause() != null) {
                completableFuture.completeExceptionally(new RuntimeException("Error during connection to server", c.cause()));
            }
            Channel channel = channelFuture.channel();
            channel.attr(CHANNEL_KEY).set(metric);
            map.put(metric, completableFuture);
            channelFuture.channel().writeAndFlush(wrappedBuffer((metric + '\n').getBytes(UTF_8)));
        });
        return completableFuture;
    }
}
