package zabbixagent.client;


import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zabbixagent.client.request.Metric;
import zabbixagent.client.request.Target;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class SpringMapProvider {
    private Map<String, Pair<Target, Metric>> metrics = new ConcurrentHashMap<>();
    private Map<String, ZabbixAgentClient.Callback> callbacks = new ConcurrentHashMap<>();

    @Bean(name = "metrics")
    public Map<String, Pair<Target, Metric>> getMetrics() throws Exception {
        return metrics;
    }

    @Bean(name = "callbacks")
    public Map<String, ZabbixAgentClient.Callback> getCallbacks() throws Exception {
        return callbacks;
    }

}