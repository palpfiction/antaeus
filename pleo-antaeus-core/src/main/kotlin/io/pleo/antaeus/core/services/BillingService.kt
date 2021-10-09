package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.CurrencyConverter
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal,
    private val currencyConverter: CurrencyConverter
) {
    /**
     * Try to charge `invoice`.
     *
     * @return The result invoice. It will have its status updated
     * either to `PAID`, `PAYMENT_FAILED` or`INCONSISTENT_DATA`, depending on the outcome
     * of the operation.
     */
    @Throws(NetworkException::class)
    fun charge(invoice: Invoice): Invoice {
        /**
         * Here we catch all exceptions. Since we have all
         * information right here (customer, its currency) it makes
         * sense not to happily send every request to the payment provider,
         * only the ones we are sure could be successful (except for network errors)
         */
        try {
            val customer = dal.fetchCustomer(invoice.customerId)
                ?: return copyInvoiceWithStatus(invoice, InvoiceStatus.INCONSISTENT_DATA)

            if (customer.currency != invoice.amount.currency) {
                val money = currencyConverter.convert(invoice.amount, customer.currency)
                return attemptToCharge(Invoice(invoice.id, invoice.customerId, money, invoice.status))
            }

            return attemptToCharge(invoice)
        } catch (customerNotFound: CustomerNotFoundException) {
            return copyInvoiceWithStatus(invoice, InvoiceStatus.INCONSISTENT_DATA)
        } catch (currencyMismatch: CurrencyMismatchException) {
            // Inconsistent data because if this happens, it means we were not
            // able to convert the currency, so manual changes must be made
            return copyInvoiceWithStatus(invoice, InvoiceStatus.INCONSISTENT_DATA)
        } catch (networkProblem: NetworkException) {
            // explicitly rethrow it, for maintainability
            throw networkProblem
        }
    }

    private fun copyInvoiceWithStatus(invoice: Invoice, status: InvoiceStatus) =
        Invoice(invoice.id, invoice.customerId, invoice.amount, status)

    private fun attemptToCharge(invoice: Invoice): Invoice {
        if (paymentProvider.charge(invoice)) return copyInvoiceWithStatus(invoice, InvoiceStatus.PAID)
        return copyInvoiceWithStatus(invoice, InvoiceStatus.PAYMENT_FAILED)

    }
}
