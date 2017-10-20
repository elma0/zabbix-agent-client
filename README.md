Java client for zabbix_agent

Usage:
```
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("zabbix-agent-client.xml");
    ZabbixAgentClient client = ctx.getBean(ZabbixAgentClient.class);
    client.fetch(new Target("127.0.0.1", 10050), Lists.newArrayList(new Metric("agent.ping")), 100, new ZabbixAgentClient.Callback() {
        @Override
        public void onEvent(Target target, Metric metric, String value) {
            System.out.println("Response for " + metric.getKey() + " from " + target.getHostname() + ":" + target.getPort() + " is " + value);
        }

        @Override
        public void onError(Target target, Metric metric, Throwable e) {
            System.out.println(e.getMessage());
        }
    });
    client.shootdown()
```