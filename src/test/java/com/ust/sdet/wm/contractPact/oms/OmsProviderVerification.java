package com.ust.sdet.wm.contractPact.oms;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
// import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Provider("oms-provider")
@PactBroker(url = "http://127.0.0.1:9292", enablePendingPacts = "true",
        providerBranch = "main", includeWipPactsSince = "2026-06-26")
public class OmsProviderVerification {

    @RegisterExtension
    private static final WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder()
                .mainBranch();
    }

    @BeforeEach
    void setup(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("127.0.0.1", wireMock.getPort()));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("Order 123 exists")
    void isOrderExists() {

        wireMock.stubFor(get(urlEqualTo("/order/123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id":123,
                                  "status":"CONFIRMED",
                                  "total":42.0
                                }
                                """)));
    }

    @State("Sku-9 has stock")
    void hasStock() {

        wireMock.stubFor(get(urlEqualTo("/order/7"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id":7,
                                  "status":"Confirmed",
                                  "total":42.0
                                }
                                """)));
    }

    @State("Provider can create orders")
    void createOrder() {

        wireMock.stubFor(post(urlEqualTo("/order"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "statusCode":201,
                                  "orderId":123,
                                  "status":"CREATED",
                                  "total":42.0
                                }
                                """)));
    }
}