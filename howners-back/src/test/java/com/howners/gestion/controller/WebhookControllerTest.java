package com.howners.gestion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.howners.gestion.service.contract.ContractESignatureService;
import com.howners.gestion.service.payment.PaymentService;
import com.howners.gestion.service.payment.StripeEventProcessor;
import com.howners.gestion.service.payments.StripeConnectService;
import com.howners.gestion.service.subscription.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebhookControllerTest {
    final SubscriptionService subscriptions = mock(SubscriptionService.class);
    final StripeEventProcessor processor = mock(StripeEventProcessor.class);
    final WebhookController controller = new WebhookController(mock(ContractESignatureService.class),
            mock(PaymentService.class), processor, subscriptions, mock(StripeConnectService.class), new ObjectMapper());

    @Test void invalidSignatureIsRejected() throws Exception {
        ReflectionTestUtils.setField(controller, "stripeWebhookSecret", "test-secret");
        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/api/webhooks/stripe").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(processor);
    }

    @Test void missingSecretNeverAcceptsUnsignedEvents() throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/api/webhooks/stripe").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(processor);
    }

    @Test void subscriptionFailureReturnsRetryableError() throws Exception {
        ReflectionTestUtils.setField(controller, "stripeWebhookSecret", "test-secret");
        doAnswer(call -> { ((Runnable) call.getArgument(1)).run(); return null; })
                .when(processor).process(any(), any());
        doThrow(new IllegalStateException("Database unavailable")).when(subscriptions)
                .processSubscriptionWebhook(anyString(), anyString(), anyString(), any(), any(), any());
        String payload = """
                {"id":"evt_test","type":"customer.subscription.updated",
                 "data":{"object":{"id":"sub_test","customer":"cus_test","object":"subscription"}}}
                """;
        long now = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal((now + "." + payload).getBytes(StandardCharsets.UTF_8)));
        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/api/webhooks/stripe").contentType("application/json").content(payload)
                        .header("Stripe-Signature", "t=" + now + ",v1=" + signature))
                .andExpect(status().isInternalServerError());
    }
}
