package com.testcontainers.demo;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

import java.net.URI;

@Component
public class KMSManagementRoute extends RouteBuilder {

    private static final String url = "http://127.0.0.1:";
    private static final String AWS_REGION = "us-east-1";

    @Override
    public void configure() throws Exception {

        restConfiguration()
                .component("jetty")
                .host("0.0.0.0")
                .port(8082)
                .bindingMode(RestBindingMode.json)
                .enableCORS(true);

        rest("create")
                .post()
                .to("direct:createKmsKey");

        rest("/list")
                .post()
                .to("direct:listKmsKeys");

        rest("/disable")
                .post()
                .to("direct:disableKmsKey");

        rest("/enable")
                .post()
                .to("direct:enableKmsKey");

        from("direct:createKmsKey")
                .routeId("CreateKmsKey")
                .process(exchange -> {
                    KmsClient kmsClient = KmsClient.builder()
                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                            .endpointOverride(URI.create(url + exchange.getIn().getHeader("port", String.class)))
                            .region(Region.of(AWS_REGION))
                            .build();
                    CreateKeyRequest request = CreateKeyRequest.builder().build();
                    CreateKeyResponse response = kmsClient.createKey(request);
                    String keyId = response.keyMetadata().keyId();
                    exchange.getMessage().setBody("Created KMS Key ID: " + keyId);
                })
                .to("log:createdKmsKey");

        //list kms key
        from("direct:listKmsKeys")
                .routeId("ListKmsKey")
                .process(exchange -> {
                    KmsClient kmsClient = KmsClient.builder()
                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                            .endpointOverride(URI.create(url + exchange.getIn().getHeader("port", String.class)))
                            .region(Region.of(AWS_REGION))
                            .build();
                    ListKeysResponse response = kmsClient.listKeys(ListKeysRequest.builder().build());
                    StringBuilder keysList = new StringBuilder();
                    for (KeyListEntry entry : response.keys()) {
                        keysList.append("Key ID: ").append(entry.keyId()).append("\n");
                    }
                    exchange.getMessage().setBody(keysList.toString());
                })
                .to("log:listOfKmsKeys");

        //disable kms key
        from("direct:disableKmsKey")
                .routeId("DisableKmsKey")
                .process(exchange -> {
                    KmsClient kmsClient = KmsClient.builder()
                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                            .endpointOverride(URI.create(url + exchange.getIn().getHeader("port", String.class)))
                            .region(Region.of(AWS_REGION))
                            .build();
                    DisableKeyRequest request = DisableKeyRequest.builder()
                            .keyId(exchange.getIn().getHeader("keyId", String.class))
                            .build();
                    System.out.println("DisableKmsKey " + exchange.getIn().getHeader("keyId", String.class) + " successfully");
                    kmsClient.disableKey(request);
                    exchange.getMessage().setBody("DisableKmsKey " + exchange.getIn().getHeader("keyId", String.class) + " successfully");
                })
                .to("log:disableKmsKey");

        //enable kms key
        from("direct:enableKmsKey")
                .routeId("EnableKmsKey")
                .process(exchange -> {
                    KmsClient kmsClient = KmsClient.builder()
                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                            .endpointOverride(URI.create(url + exchange.getIn().getHeader("port", String.class)))
                            .region(Region.of(AWS_REGION))
                            .build();
                    EnableKeyRequest enableKeyRequest = EnableKeyRequest.builder()
                            .keyId(exchange.getIn().getHeader("keyId", String.class))
                            .build();
                    System.out.println("EnableKmsKey " + exchange.getIn().getHeader("keyId", String.class) + " successfully");
                    kmsClient.enableKey(enableKeyRequest);
                    exchange.getMessage().setBody("EnableKmsKey " + exchange.getIn().getHeader("keyId", String.class) + " successfully");
                })
                .to("log:enableKmsKey");
    }
}
