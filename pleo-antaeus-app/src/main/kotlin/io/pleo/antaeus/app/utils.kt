import io.pleo.antaeus.core.events.EventHandler
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.CurrencyConverter
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.events.Event
import mu.KotlinLogging
import java.math.BigDecimal
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            if (Random.nextDouble() > 0.7) throw NetworkException()
            return Random.nextBoolean()
        }
    }
}

internal fun getCurrencyConverter(): CurrencyConverter {
    return object : CurrencyConverter {
        override fun convert(money: Money, currency: Currency): Money {
            return Money(money.value * BigDecimal.valueOf(Random.nextDouble()), currency)
        }

    }
}

internal fun getEventHandler(): EventHandler {
    return object : EventHandler {
        override fun handle(event: Event) {
            logger.info { event }
        }
    }
}