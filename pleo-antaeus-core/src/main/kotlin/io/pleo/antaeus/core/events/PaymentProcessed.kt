package io.pleo.antaeus.core.events

import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.events.Event

data class PaymentProcessed(val invoice: Invoice) : Event