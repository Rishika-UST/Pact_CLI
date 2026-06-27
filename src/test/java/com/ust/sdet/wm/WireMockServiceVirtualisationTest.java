package com.ust.sdet.wm;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static io.restassured.RestAssured.given;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WireMockServiceVirtualisationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private HttpClient client;

    @BeforeEach
    void setup() {
        io.restassured.RestAssured.baseURI = wm.baseUrl();
        client = HttpClient.newHttpClient();
    }

    @Test
    @DisplayName("Stub Inventory")
    void returnsConfirmedOrderOverRealHttp() {

        wm.stubFor(get(urlEqualTo("/orders/123"))
                .willReturn(okJson("""
                        {
                          "id":123,
                          "status":"CONFIRMED",
                          "total":42.0
                        }
                        """)));

        given()
                .when()
                .get("/orders/123")
                .then()
                .statusCode(200)
                .body("id", equalTo(123))
                .body("status", equalTo("CONFIRMED"))
                .body("total", equalTo(42.0f));

        wm.verify(exactly(1),
                getRequestedFor(urlEqualTo("/orders/123")));
    }

    @Test
    @DisplayName("Stub Inventory - Success and Out Of Stock")
    void stubInventoryTwoOutcomes() {

        wm.stubFor(get(urlEqualTo("/inventory/SKU-9"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                   "sku":"SKU-9",
                                   "qty":5
                                }
                                """)));

        wm.stubFor(get(urlEqualTo("/inventory/SKU-0"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                   "error":"OUT_OF_STOCK"
                                }
                                """)));

        given()
                .when()
                .get("/inventory/SKU-9")
                .then()
                .statusCode(200)
                .body("sku", equalTo("SKU-9"))
                .body("qty", equalTo(5));

        given()
                .when()
                .get("/inventory/SKU-0")
                .then()
                .statusCode(409)
                .body("error", equalTo("OUT_OF_STOCK"));

        wm.verify(exactly(1),
                getRequestedFor(urlEqualTo("/inventory/SKU-9")));

        wm.verify(exactly(1),
                getRequestedFor(urlEqualTo("/inventory/SKU-0")));
    }

    @Test
    @DisplayName("Exercise 2 - Timeout")
    void exercise2() {

        wm.stubFor(get(urlEqualTo("/orders/slow"))
                .willReturn(ok()
                        .withFixedDelay(3000)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wm.baseUrl() + "/orders/slow"))
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();

        assertThrows(HttpTimeoutException.class,
                () -> client.send(request, ofString()));
    }

    @Test
    @DisplayName("Exercise 3 - Stateful Behaviour")
    void exercise3() {

        wm.stubFor(get(urlEqualTo("/orders/42"))
                .inScenario("Order Flow")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson("""
                        {
                           "order":42,
                           "status":"PENDING"
                        }
                        """))
                .willSetStateTo("CONFIRMED"));

        wm.stubFor(get(urlEqualTo("/orders/42"))
                .inScenario("Order Flow")
                .whenScenarioStateIs("CONFIRMED")
                .willReturn(okJson("""
                        {
                           "order":42,
                           "status":"CONFIRMED"
                        }
                        """)));

        given()
                .when()
                .get("/orders/42")
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING"));

        given()
                .when()
                .get("/orders/42")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"));

        wm.verify(exactly(2),
                getRequestedFor(urlEqualTo("/orders/42")));
    }
}