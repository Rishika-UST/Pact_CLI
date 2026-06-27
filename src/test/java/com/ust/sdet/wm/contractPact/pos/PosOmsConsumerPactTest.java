package com.ust.sdet.wm.contractPact.pos;

import com.ust.sdet.wm.contractPact.pos.OmsClient;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;

import io.restassured.response.Response;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(
        providerName = "oms-provider",
        pactVersion = PactSpecVersion.V4
)
class PosOmsConsumerPactTest {
    @Pact(provider = "oms-provider", consumer = "pos-consumer")
    V4Pact getOrder(PactDslWithProvider builder) {

        return builder
                .given("Order 123 exists")

                .uponReceiving("a request for order 123")
                .path("/order/123")
                .method("GET")
                .willRespondWith()
                .status(200)
                .matchHeader(
                        "Content-Type",
                        "application/json(;.*)?",
                        "application/json")

                .body(new PactDslJsonBody()
                        .integerType("id", 123)
                        .stringType("status", "CONFIRMED")
                        .numberType("total", 42.0))

                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getOrder")
    void testGetOrder(MockServer mockServer) {

        Response response =
                given()
                        .baseUri(mockServer.getUrl())
                        .when()
                        .get("/order/123");
        response.then()
                .statusCode(200);
        response.then().log().all();
    }

    @Disabled
    @Pact(provider = "oms-provider", consumer = "pos-consumer")
    V4Pact getInventoryShow(PactDslWithProvider builder) {

        return builder
                .given("Sku-9 has stock")
                .uponReceiving("a request for Sku-9")
                .path("/order/7")
                .method("GET")
                .willRespondWith()
                .status(200)
                .matchHeader(
                        "Content-Type",
                        "application/json(;.*)?",
                        "application/json")
                .body(new PactDslJsonBody()
                        .integerType("id", 7)
                        .stringType("status", "Confirmed")
                        .numberType("total", 42))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getInventoryShow")
    void testGetInventory(MockServer mockServer) {

        Response response =
                given()
                        .baseUri(mockServer.getUrl())
                        .when()
                        .get("/order/7");
        response.then()
                .statusCode(200);
        response.then().log().all();
    }

    @Disabled
    @Pact(provider = "oms-provider", consumer = "pos-consumer")
    V4Pact createOrder(PactDslWithProvider builder) {
        return builder
                .given("Provider can create orders")
                .uponReceiving("a request to create an order")
                .path("/order")
                .method("POST")
                .matchHeader(
                        "Content-Type",
                        "application/json(;.*)?",
                        "application/json")
                .body(new PactDslJsonBody()
                        .integerType("statuscode", 0)
                        .integerType("orderId", 123)
                        .stringType("status", "NEW")
                        .numberType("total", 42.0))
                .willRespondWith()
                .status(201)
                .matchHeader(
                        "Content-Type",
                        "application/json(;.*)?",
                        "application/json")
                .body(new PactDslJsonBody()
                        .integerType("statuscode", 201)
                        .integerType("orderId", 123)
                        .stringType("status", "CREATED")
                        .numberType("total", 42.0))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createOrder")
    void testCreateOrder(MockServer mockServer) {

        OmsClient omsClient =
                new OmsClient(mockServer.getUrl());
        OmsClient.Order request =
                new OmsClient.Order(
                        0,
                        123,
                        "NEW",
                        42.0);
        OmsClient.Order response =
                omsClient.createOrder(request);
        assertEquals(123, response.orderId());
        assertEquals("CREATED", response.status());
        assertEquals(42.0, response.total());
    }


}