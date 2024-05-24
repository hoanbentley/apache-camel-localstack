package com.testcontainers.demo;

import org.springframework.boot.SpringApplication;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.DisableKeyRequest;

import java.util.UUID;

public class TestApplication {

    public static void main(String[] args) throws Exception {
        setup();
        SpringApplication.from(Application::main).run(args);
    }

    static void setup() {
        try {
            var container = new LocalStackContainer(
                    DockerImageName.parse("localstack/localstack:latest")
            );

            container.start();
            Integer kmsPort = container.getMappedPort(4566);
            System.out.println("Ryan debug: http://" + container.getContainerIpAddress() + ":" + kmsPort);

            String BUCKET_NAME = UUID.randomUUID().toString();
            String QUEUE_NAME = UUID.randomUUID().toString();

            container.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
            container.execInContainer("awslocal", "sqs","create-queue","--queue-name",QUEUE_NAME);
            //container.execInContainer("awslocal", "kms","create-key");

            System.setProperty("app.bucket", BUCKET_NAME);
            System.setProperty("app.queue", QUEUE_NAME);
            System.setProperty("spring.cloud.aws.region.static", container.getRegion());
            System.setProperty("spring.cloud.aws.credentials.access-key", container.getAccessKey());
            System.setProperty("spring.cloud.aws.credentials.secret-key", container.getSecretKey());
            System.setProperty("spring.cloud.aws.endpoint", container.getEndpoint().toString());
            /*
            // create key
            KmsClient kmsClient = KmsClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(container.getAccessKey(), container.getSecretKey())))
                    .region(Region.of(container.getRegion()))
                    .endpointOverride(container.getEndpointOverride(LocalStackContainer.Service.KMS))
                    .build();

            //System.out.println("Ryan debug:" + container.getEndpointOverride(LocalStackContainer.Service.KMS));

            CreateKeyRequest request = CreateKeyRequest.builder().build();
            CreateKeyResponse response = kmsClient.createKey(request);
            String keyId = response.keyMetadata().keyId();
            System.out.println("Ryan new:" + keyId);

            kmsClient.listKeys().keys().forEach(System.out::println);*/

            // disable key
            /*KmsClient kmsClient = KmsClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(container.getAccessKey(), container.getSecretKey())))
                    .region(Region.of(container.getRegion()))
                    .endpointOverride(container.getEndpointOverride(LocalStackContainer.Service.KMS))
                    .build();

            DisableKeyRequest request = DisableKeyRequest.builder()
                    .keyId(null)
                    .build();
            kmsClient.disableKey(request);*/

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
