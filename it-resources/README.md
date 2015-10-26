# Integration test logging


We ignore 'application-logger.xml' in this directory.
If you want to add some logging to your integration tests, create a file with that name and configure your loggers.
Only you'll see the changes in the logs.

Here's an example to get you started:


```xml
    <configuration>
        <conversionRule conversionWord="coloredLevel" converterClass="play.api.Logger$ColoredLevel"/>

        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%coloredLevel %logger{55} - %message%n%xException{5}</pattern>
            </encoder>
        </appender>

        <logger name="ch.qos.logback" level="WARN"/>
        <logger name="org.corespring.services.salat.ServicesContext" level="WARN"/>
        <logger name="org.corespring.it" level="DEBUG"/>
        <logger name="org.corespring.v2.api" level="DEBUG"/>

        <root level="INFO">
            <appender-ref ref="STDOUT" />
        </root>
    </configuration>
```