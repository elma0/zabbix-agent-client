Java client for zabbix_agent

Usage:
```
    ZabbixAgentClient zabbixAgentClient = new ZabbixAgentClient(new NioEventLoopGroup(1));
    zabbixAgentClient.fetch(host, port, metrics).get();
```