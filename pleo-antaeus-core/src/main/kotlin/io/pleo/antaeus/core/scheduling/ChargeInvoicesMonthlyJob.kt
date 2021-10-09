package io.pleo.antaeus.core.scheduling

import io.pleo.antaeus.core.exceptions.MissingDependenciesException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.quartz.Job
import org.quartz.JobExecutionContext
import kotlinx.coroutines.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ChargeInvoicesMonthlyJob(
    // public variables so that Quartz can initialize them using JobData
    var billingService: BillingService? = null,
    var dal: AntaeusDal? = null
) : Job {

    private companion object {
        const val MAX_PROCESSES = 10
        const val MAX_RETRIES = 3
    }

    @Throws(MissingDependenciesException::class)
    override fun execute(context: JobExecutionContext?) {
        if (billingService == null || dal == null) throw MissingDependenciesException()

        val pendingInvoices = dal!!.fetchInvoicesByStatus(InvoiceStatus.PENDING)
        if (pendingInvoices.isEmpty()) return

        val chunkSize = (pendingInvoices.size / MAX_PROCESSES).coerceAtLeast(pendingInvoices.size)

        runBlocking {
            pendingInvoices.chunked(chunkSize).forEach {
                launch {
                    it.forEach {
                        attemptPayment(it)
                    }
                }
            }
        }
    }

    private suspend fun attemptPayment(invoice: Invoice, retries: Int = 0) {
        try {
            val chargedInvoice = billingService!!.charge(invoice)
            dal!!.updateInvoice(chargedInvoice)
        } catch (networkException: NetworkException) {
            if (retries == MAX_RETRIES) return
            delay(1000)
            attemptPayment(invoice, retries + 1)
        }
    }
}