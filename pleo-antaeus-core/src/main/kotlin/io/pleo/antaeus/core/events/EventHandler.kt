package io.pleo.antaeus.core.events

import io.pleo.antaeus.models.events.Event

/**
 * This would be our event handler. It handles
 * our domain events and does something with them.
 * Ideally it could be a chain of different event handlers,
 * for example, one dispatches events to a Kafka topic,
 * another logs the event to console...
 */
interface EventHandler {
    fun handle(event: Event)
}