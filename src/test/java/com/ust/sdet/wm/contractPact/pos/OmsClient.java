package com.ust.sdet.wm.contractPact.pos;


import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class OmsClient {

    private final String baseUrl;

    public OmsClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public CreateOrderResponse createOrder(CreateOrderRequest order) {

        return given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(order)
                .when()
                .post("/order")
                .then()
                .statusCode(201)
                .extract()
                .as(CreateOrderResponse.class);
    }

    public Order getOrder(int orderId) {

        Response response = given()
                .baseUri(baseUrl)
                .when()
                .get("/order/" + orderId);

        response.then().statusCode(200);

        return response.as(Order.class);
    }

    public record Order(
            int id,
            String status,
            double total
    ) {}

    public record CreateOrderRequest(
            int statusCode,
            int orderId,
            String status,
            double total
    ) {}

    public record CreateOrderResponse(
            int statusCode,
            int orderId,
            String status,
            double total
    ) {}
}