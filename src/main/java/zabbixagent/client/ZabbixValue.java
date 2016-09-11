package zabbixagent.client;

import org.springframework.context.ApplicationEvent;


public class ZabbixValue extends ApplicationEvent {

    public ZabbixValue(ChannelValuePair source) {
        super(source);
    }

}
