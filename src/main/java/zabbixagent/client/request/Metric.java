package zabbixagent.client.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonTypeName("Metric")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = Metric.class)
public class Metric {
    private String key;
    private String name;

    public Metric(String key) {
        this.key = key;
    }

    @JsonCreator
    public Metric(@Nonnull @JsonProperty("key") String key, @Nullable @JsonProperty("name") String name) {
        this.key = key;
        this.name = name;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nonnull
    public String getKey() {
        return key;
    }


}
