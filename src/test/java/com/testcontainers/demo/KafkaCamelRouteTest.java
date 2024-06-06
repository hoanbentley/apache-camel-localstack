package com.testcontainers.demo;
import com.testcontainers.demo.dto.Passenger;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;


class KafkaCamelRouteTest extends CamelTestSupport {

    private static final Logger log = LoggerFactory.getLogger(KafkaCamelRouteTest.class);
    private static KafkaContainer kafkaContainer;

    @EndpointInject("mock:result")
    private MockEndpoint mockResult;

    @BeforeAll
    public static void startKafkaContainer() {
        kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.2.1")
        );
        kafkaContainer.start();
    }

    @AfterAll
    public static void stopKafkaContainer() {
        if (kafkaContainer != null) {
            kafkaContainer.stop();
        }
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();
        String kafkaBrokerAddress = kafkaContainer.getBootstrapServers();
        String kafkaUri = "kafka:test-topic?brokers=" + kafkaBrokerAddress;
        Endpoint endpoint = context.getEndpoint(kafkaUri);
        context.addEndpoint("kafka:test-topic", endpoint);

        return context;
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new KafkaCamelRoute(kafkaContainer.getBootstrapServers());
    }

    @Test
    public void testKafkaCamelIntegration() throws Exception {
        CamelContext context = context();
        context.start();
        try {
            // given
            Passenger passenger = Passenger.builder()
                .firstName("Test ABC")
                .age(37)
                .gender(true)
                .build();

            // expect the mock endpoint to receive the message
            mockResult.expectedMessageCount(1);
            mockResult.expectedBodiesReceived(passenger);

            // when
            ProducerTemplate producerTemplate = context.createProducerTemplate();
            producerTemplate.sendBody("direct:start", passenger);

            // then
            mockResult.assertIsSatisfied();

            Passenger messPassenger = mockResult.getReceivedExchanges().get(0).getIn().getBody(Passenger.class);
            log.info("Message of passenger {}", messPassenger);

        } finally {
            context.stop();
        }
    }
}
