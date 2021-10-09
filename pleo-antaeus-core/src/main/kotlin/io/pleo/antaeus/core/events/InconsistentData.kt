package io.pleo.antaeus.core.events

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.events.Event

data class InconsistentData(val invoice: Invoice, val customer: Customer?) : Event