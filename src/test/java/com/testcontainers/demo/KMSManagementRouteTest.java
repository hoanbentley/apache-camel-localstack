package com.testcontainers.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

@Testcontainers
class KMSManagementRouteTest {

    @Container
    public static LocalStackContainer localStackContainer = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:latest")
    )
            .withServices(LocalStackContainer.Service.KMS);

    @DisplayName("JUnit test for create kms key operation")
    @Test
    public void testCreateKmsKey() {
        CamelContext context = new DefaultCamelContext();
        // Start the Camel context
        context.start();

        // given
        KmsClient kmsClient = KmsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.KMS))
                .region(Region.US_EAST_1)
                .build();

        // when
        CreateKeyResponse response = kmsClient.createKey();

        // then
        assertNotNull(response.keyMetadata().keyId());

        // Stop the Camel context
        context.stop();
    }

    @DisplayName("JUnit test for list kms keys operation")
    @Test
    public void testListKmsKey() {
        CamelContext context = new DefaultCamelContext();

        // Start the Camel context
        context.start();

        // given
        KmsClient kmsClient = KmsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.KMS))
                .region(Region.US_EAST_1)
                .build();

        // when
        kmsClient.createKey();
        ListKeysResponse response = kmsClient.listKeys(ListKeysRequest.builder().build());

        // then
        assertNotNull(response.keys());
        assertThat(response.keys().size()).isGreaterThan(0);

        // Stop the Camel context
        context.stop();
    }

    @DisplayName("JUnit test for disable kms key operation")
    @Test
    public void testDisableKmsKey() {
        CamelContext context = new DefaultCamelContext();
        // Start the Camel context
        context.start();

        // given
        KmsClient kmsClient = KmsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.KMS))
                .region(Region.US_EAST_1)
                .build();

        // when
        CreateKeyResponse response = kmsClient.createKey();
        //System.out.println("Ryan debug response:" + response.keyMetadata().keyId());

        DisableKeyRequest request = DisableKeyRequest.builder()
                .keyId(response.keyMetadata().keyId())
                .build();
        kmsClient.disableKey(request);
        //System.out.println("Ryan debug disabled successfully");

        DescribeKeyRequest describeKeyRequest = DescribeKeyRequest.builder()
                .keyId(response.keyMetadata().keyId())
                .build();

        DescribeKeyResponse describeKeyResponse = kmsClient.describeKey(describeKeyRequest);
        System.out.println("Log debug DescribeKeyResponse:" + describeKeyResponse.keyMetadata());

        // then
        assertNotNull(describeKeyResponse.keyMetadata().keyId());
        assertThat(describeKeyResponse.keyMetadata().enabled()).isEqualTo(false);

        // Stop the Camel context
        context.stop();
    }

    @DisplayName("JUnit test for enable kms key operation")
    @Test
    public void testEnableKmsKey() {
        CamelContext context = new DefaultCamelContext();
        // Start the Camel context
        context.start();

        // given
        KmsClient kmsClient = KmsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.KMS))
                .region(Region.US_EAST_1)
                .build();

        // when
        // create key
        CreateKeyResponse response = kmsClient.createKey();
        //System.out.println("Ryan debug response:" + response.keyMetadata().keyId());

        // disable key
        DisableKeyRequest request = DisableKeyRequest.builder()
                .keyId(response.keyMetadata().keyId())
                .build();
        kmsClient.disableKey(request);
        //System.out.println("Ryan debug disabled successfully");

        // enable key
        EnableKeyRequest enableKeyRequest = EnableKeyRequest.builder()
                .keyId(response.keyMetadata().keyId())
                .build();
        kmsClient.enableKey(enableKeyRequest);
        //System.out.println("Ryan debug enabled successfully");

        // describe key
        DescribeKeyRequest describeKeyRequest = DescribeKeyRequest.builder()
                .keyId(response.keyMetadata().keyId())
                .build();

        DescribeKeyResponse describeKeyResponse = kmsClient.describeKey(describeKeyRequest);
        System.out.println("Log debug DescribeKeyResponse:" + describeKeyResponse.keyMetadata());
        KeyMetadata keyMetadata = describeKeyResponse.keyMetadata();

        // then
        assertNotNull(describeKeyResponse.keyMetadata().keyId());
        assertThat(describeKeyResponse.keyMetadata().enabled()).isEqualTo(true);

        // Stop the Camel context
        context.stop();
    }
}
