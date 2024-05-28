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
import org.testcontainers.containers.Container.ExecResult;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaCamelKraftModeRouteTest {

    private static KafkaContainer kafkaContainer;
    private static String bootstrapServers;

    @BeforeAll
    public static void setup() throws IOException, InterruptedException {
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.2.1"))
                .withKraft();
                /*.withEnv("KAFKA_NODE_ID", "1")
                .withEnv("KAFKA_PROCESS_ROLES", "broker,controller")
                .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
                .withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093")
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
                .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT")
                .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@localhost:9093")
                .withEnv("KAFKA_LOG_DIRS", "/tmp/kraft-combined-logs")
                .withExposedPorts(9092, 9093);*/

        kafkaContainer.start();

        // Format the storage directory
        /*ExecResult result = kafkaContainer.execInContainer(
                "kafka-storage.sh",
                "format",
                "-t", "test-uuid",
                "-c", "/etc/kafka/kafka.properties"
        );

        if (result.getExitCode() != 0) {
            throw new RuntimeException("Failed to format Kafka storage: " + result.getStderr());
        }*/

        bootstrapServers = kafkaContainer.getBootstrapServers();//.replace("PLAINTEXT://", "");
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
                from("kafka:input-topic?brokers=" + bootstrapServers + "&autoOffsetReset=earliest&groupId=test-group")
                        .log("Received message from Kafka: ${body}")
                        .process(exchange -> {
                            String message = exchange.getIn().getBody(String.class);
                            exchange.getIn().setBody("Processed: " + message);
                        })
                        .to("kafka:output-topic?brokers=" + bootstrapServers);
            }
        });

        camelContext.start();

        camelContext.createProducerTemplate().sendBody("kafka:input-topic?brokers=" + bootstrapServers, "Hello Kafka!");

        DefaultConsumerTemplate consumerTemplate = new DefaultConsumerTemplate(camelContext);
        consumerTemplate.start();
        String message = consumerTemplate.receiveBody("kafka:output-topic?brokers=" + bootstrapServers, String.class);
        consumerTemplate.stop();

        assert message.equals("Processed: Hello Kafka!");

        camelContext.stop();
    }
}
