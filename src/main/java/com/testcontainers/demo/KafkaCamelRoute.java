package com.testcontainers.demo;

import com.testcontainers.demo.dto.Passenger;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

@Component
public class KafkaCamelRoute extends RouteBuilder {

    private static String bootstrapServers;
    private static final String topicName = "test-topic";

    public KafkaCamelRoute(String bootstrapServers) {
        KafkaCamelRoute.bootstrapServers = bootstrapServers;
    }

    @Override
    public void configure() throws Exception {
        // Route to produce messages to Kafka
        from("direct:start")
            .marshal().json(JsonLibrary.Jackson)
            .to("kafka:" + topicName + "?brokers=" + bootstrapServers);

        // Route to consume messages from Kafka
        from("kafka:" + topicName + "?brokers=" + bootstrapServers + "&autoOffsetReset=earliest&groupId=test-group")
            .unmarshal().json(JsonLibrary.Jackson, Passenger.class)
            .log("Received message from Kafka: ${body}")
            .to("mock:result");
    }
}
