# hoecoga-bot
A simple scala bot for slack.

## Run
Edit `slack.base-url` and `slack.token` on `conf/application.conf`.

```
sbt assembly
java -Dconfig.file=conf/application.conf -Dlogback.configurationFile=conf/logback.xml -jar target/scala-2.11/hoecoga-bot-assembly-1.0.jar
```

## Commands
### Ping
```
ping -- sends back a pong message
Usage: ping [options]

 -h | --help
       prints this usage text
```

### Cron
```
cron -- daemons to send scheduled simple messages
Usage: cron [list|create|delete] [options] <args>...

 -h | --help
       prints this usage text
Command: list
prints cron jobs
Command: create <cron expr>...
creates a new cron job
 <cron expr>...
       seconds, minutes, hours, day-of-month, month, day-of-week, and simple text, respectively (for more detail, refer to http://www.quartz-scheduler.org/generated/2.2.1/html/qs-all/#page/Quartz_Scheduler_Documentation_Set/co-trg_crontriggers.html)
Command: delete <id>
deletes a cron job
 <id>
       job identifier
```
