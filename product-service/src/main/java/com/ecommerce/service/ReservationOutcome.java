package com.ecommerce.service;

/**
 * What reserving stock for an order came to. Sealed so the SAGA reply side has to
 * handle both, and so the rejection reason travels with the rejection instead of
 * living in a field that is null in every other state.
 */
public sealed interface ReservationOutcome {

    long orderId();

    record Reserved(long orderId) implements ReservationOutcome {}

    record Rejected(long orderId, String reason) implements ReservationOutcome {}
}
