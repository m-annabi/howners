package com.howners.gestion.service.payment;

import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** La contrainte unique sérialise les doublons ; un échec annule aussi l'enregistrement. */
@Service
@RequiredArgsConstructor
public class StripeEventProcessor {
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void process(Event event, Runnable handler) {
        if (event.getId() == null || event.getId().isBlank()) {
            throw new IllegalArgumentException("Identifiant d'événement Stripe absent");
        }
        int inserted = jdbcTemplate.update("""
                INSERT INTO stripe_processed_events (event_id, event_type, processed_at)
                VALUES (?, ?, CURRENT_TIMESTAMP) ON CONFLICT (event_id) DO NOTHING
                """, event.getId(), event.getType());
        if (inserted == 1) handler.run();
    }
}
