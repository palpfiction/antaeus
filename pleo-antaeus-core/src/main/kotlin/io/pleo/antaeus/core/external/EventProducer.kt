package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.events.Event

interface EventProducer {

    fun publish(event: Event)
}