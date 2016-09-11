package zabbixagent.client.response;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import static java.lang.Byte.SIZE;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static zabbixagent.client.response.ZabbixHeaderDecoder.DecodingState.DATA;
import static zabbixagent.client.response.ZabbixHeaderDecoder.DecodingState.HEADER;
import static zabbixagent.client.response.ZabbixHeaderDecoder.DecodingState.VERSION;


public class ZabbixHeaderDecoder extends ReplayingDecoder<ZabbixHeaderDecoder.DecodingState> {
    public enum DecodingState {
        HEADER,
        VERSION,
        DATA_LENGTH,
        DATA,
    }
    private Long dataLength;
    private static final String ZBX_HEADER = "ZBXD";
    private static final String HEADER_VERSION = "\001";
    private static final int DATA_LENGTH = 8;        
    private static final Logger LOGGER = LoggerFactory.getLogger(ZabbixHeaderDecoder.class);

    public ZabbixHeaderDecoder() {
        super(HEADER);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case HEADER:
                Integer head = in.readInt();
                byte[] header = allocate(Integer.SIZE / SIZE).putInt(head).array();
                byte[] zbxHeader = ZBX_HEADER.getBytes();
                if (!Arrays.equals(header, zbxHeader)) {
                    LOGGER.error("Incorrect Header");
                    break;
                }
                checkpoint(VERSION);
            case VERSION:
                Byte ver = in.readByte();
                byte[] version = {ver};
                byte[] zbxVersion = HEADER_VERSION.getBytes();
                if (!Arrays.equals(version, zbxVersion)) {
                    LOGGER.error("Incorrect Version");
                    break;
                }
                checkpoint(DecodingState.DATA_LENGTH);
            case DATA_LENGTH:
                Long length = in.readLong();
                ByteBuffer dataLengthBuf = allocate(DATA_LENGTH).order(LITTLE_ENDIAN).putLong(length);
                dataLengthBuf.order(BIG_ENDIAN);
                dataLengthBuf.position(0);
                dataLength = dataLengthBuf.getLong();
                checkpoint(DATA);
            case DATA:
                ByteBuf frame = in.readBytes(dataLength.intValue());
                checkpoint(HEADER);
                out.add(frame);
                break;
            default:
                throw new IllegalStateException("Incorrect State");
        }
    }
}
