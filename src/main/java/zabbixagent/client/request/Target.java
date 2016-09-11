package zabbixagent.client.request;


import com.sun.istack.internal.NotNull;

public class Target {
    private Integer port;
    private String hostname;

    public Target(@NotNull String hostname, @NotNull Integer port) {
        this.port = port;
        this.hostname = hostname;
    }

    @NotNull
    public Integer getPort() {
        return port;
    }

    @NotNull
    public String getHostname() {
        return hostname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Target target = (Target) o;

        if (!hostname.equals(target.hostname)) return false;
        if (!port.equals(target.port)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = port.hashCode();
        result = 31 * result + hostname.hashCode();
        return result;
    }
}
