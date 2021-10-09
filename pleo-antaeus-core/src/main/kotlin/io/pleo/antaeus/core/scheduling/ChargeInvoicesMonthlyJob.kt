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
        logger.info { "Launched job: ${context?.jobDetail?.key}" }
        if (billingService == null || dal == null) throw MissingDependenciesException()

        val pendingInvoices = dal!!.fetchInvoicesByStatus(InvoiceStatus.PENDING)
        if (pendingInvoices.isEmpty()) return

        val chunkSize = computeChunkSize(pendingInvoices.size)

        runBlocking {
            pendingInvoices.chunked(chunkSize).forEach {
                launch {
                    it.forEach {
                        attemptPayment(it)
                    }
                }
            }
        }

        logger.info { "Finished job: ${context?.jobDetail?.key}" }
    }

    private fun computeChunkSize(size: Int): Int {
        val chunkSize = size / MAX_PROCESSES
        if (chunkSize == 0) return size

        return chunkSize
    }

    private suspend fun attemptPayment(invoice: Invoice, retries: Int = 0) {
        try {
            billingService!!.charge(invoice)
        } catch (networkException: NetworkException) {
            if (retries == MAX_RETRIES) return
            delay(1000)
            attemptPayment(invoice, retries + 1)
        }
    }
}