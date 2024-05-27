package com.testcontainers.demo;

import com.testcontainers.demo.dto.Passenger;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultConsumerTemplate;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaCamelRouteTest {

    private static KafkaContainer kafkaContainer;
    private static String bootstrapServers;
    private static final String topicName = "ryan-topic";

    @BeforeAll
    public static void setup() throws ExecutionException, InterruptedException {
        kafkaContainer = new KafkaContainer(
                DockerImageName.parse("confluentinc/cp-kafka:7.2.1")
        ).withExposedPorts(9093); // Expose Kafka port

        kafkaContainer.start();
        bootstrapServers = kafkaContainer.getBootstrapServers();

        createKafkaTopic(topicName, 1, (short) 1);
    }

    @AfterAll
    public static void tearDown() {
        kafkaContainer.stop();
    }

    @Test
    public void testKafkaCamelIntegration() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.addRoutes(new RouteBuilder() {
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
                        .to("direct:result");
            }
        });

        camelContext.start();

        // Produce a message to Kafka
        Passenger passenger = Passenger.builder()
                .firstName("Ryan Truong")
                .age(37)
                .gender(true)
                .build();

        camelContext.createProducerTemplate().sendBody("direct:start", passenger);

        // Consume the message from Kafka
        DefaultConsumerTemplate consumerTemplate = new DefaultConsumerTemplate(camelContext);
        consumerTemplate.start();

        //String message = consumerTemplate.receiveBody("kafka:" + topicName + "?brokers=" + bootstrapServers, String.class);
        Passenger message = consumerTemplate.receiveBody("direct:result", 5000, Passenger.class);
        consumerTemplate.stop();

        // Assert that the message is received
        assertThat(message).isNotNull();
        assert message.getFirstName().equals("Ryan Truong");
        //assert message.getFirstName().equals("Ryan Truong");
        //assert message.getFirstName().equals("Ryan Truong");

        camelContext.stop();
    }

    private static void createKafkaTopic(String topicName, int numPartitions, int replicationFactor)
            throws ExecutionException, InterruptedException {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(properties)) {
            NewTopic newTopic = new NewTopic(topicName, numPartitions, (short) replicationFactor);
            adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
        }
    }
}
