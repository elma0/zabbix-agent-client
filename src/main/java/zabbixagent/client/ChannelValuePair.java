package zabbixagent.client;


public class ChannelValuePair {
    private String channel;
    private String value;
    private Throwable throwable;

    public ChannelValuePair(String channel, String value) {
        this.channel = channel;
        this.value = value;
    }

    public ChannelValuePair(String channel, Throwable throwable) {
        this.channel = channel;
        this.throwable = throwable;
    }

    public String getChannel() {
        return channel;
    }

    public String getValue() {
        return value;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}