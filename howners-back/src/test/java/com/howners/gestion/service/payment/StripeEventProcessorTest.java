package com.howners.gestion.service.payment;

import com.stripe.model.Event;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StripeEventProcessorTest {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    StripeEventProcessor processor = new StripeEventProcessor(jdbc);
    Event event() {
        Event event = new Event();
        event.setId("evt_123");
        event.setType("customer.subscription.updated");
        return event;
    }
    @Test void skipsAlreadyProcessedEvent() {
        Runnable handler = mock(Runnable.class);
        processor.process(event(), handler);
        verifyNoInteractions(handler);
    }
    @Test void processesNewEvent() {
        when(jdbc.update(anyString(), eq("evt_123"), eq("customer.subscription.updated"))).thenReturn(1);
        Runnable handler = mock(Runnable.class);
        processor.process(event(), handler);
        verify(handler).run();
    }
    @Test void propagatesFailureForTransactionRollbackAndProviderRetry() {
        when(jdbc.update(anyString(), eq("evt_123"), eq("customer.subscription.updated"))).thenReturn(1);
        assertThatThrownBy(() -> processor.process(event(), () -> { throw new IllegalStateException("DB unavailable"); }))
                .isInstanceOf(IllegalStateException.class);
    }
    @Test void refusesMissingEventId() {
        assertThatThrownBy(() -> processor.process(new Event(), () -> {})).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbc);
    }
}
