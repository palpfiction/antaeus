package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Invoice

class UnableToUpdateInvoiceException(private val invoice: Invoice) : Exception("Unable to update invoice: $invoice") {
}