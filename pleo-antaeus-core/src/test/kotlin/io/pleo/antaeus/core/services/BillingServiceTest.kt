package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.CurrencyConverter
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private const val EXISTENT_CUSTOMER_ID = 222
private const val NON_EXISTENT_CUSTOMER_ID = 404

class BillingServiceTest {

    private val paymentProvider = mockk<PaymentProvider>()
    private val dal = mockk<AntaeusDal>() {
        every { fetchCustomer(EXISTENT_CUSTOMER_ID) } returns Customer(EXISTENT_CUSTOMER_ID, Currency.EUR)
        every { fetchCustomer(NON_EXISTENT_CUSTOMER_ID) } returns null
    }
    private val currencyConverter = mockk<CurrencyConverter>()
    private val billingService = BillingService(paymentProvider, dal, currencyConverter)


    @Test
    fun `If payment is successful, it should return an invoice with status PAID and update invoice in database`() {
        val expected =
            Invoice(111, EXISTENT_CUSTOMER_ID, Money(BigDecimal.valueOf(140), Currency.EUR), InvoiceStatus.PAID)
        val invoice =
            Invoice(111, EXISTENT_CUSTOMER_ID, Money(BigDecimal.valueOf(140), Currency.EUR), InvoiceStatus.PENDING)

        every { paymentProvider.charge(invoice) } returns true
        every { dal.updateInvoice(expected) } returns expected

        assertEquals(expected, billingService.charge(invoice))
        verify { dal.updateInvoice(expected) }
    }

    @Test
    fun `If payment was not successful, it should return an invoice with status FAILED`() {
        val expected = Invoice(
            111,
            EXISTENT_CUSTOMER_ID,
            Money(BigDecimal.valueOf(140), Currency.EUR),
            InvoiceStatus.PAYMENT_FAILED
        )
        val invoice =
            Invoice(111, EXISTENT_CUSTOMER_ID, Money(BigDecimal.valueOf(140), Currency.EUR), InvoiceStatus.PENDING)

        every { paymentProvider.charge(invoice) } returns false
        every { dal.updateInvoice(expected) } returns expected

        assertEquals(expected, billingService.charge(invoice))
        verify { dal.updateInvoice(expected) }

    }

    @Test
    fun `If the customer is not found, it should return an invoice with status INCONSISTENT_DATA`() {
        val expected = Invoice(
            111,
            NON_EXISTENT_CUSTOMER_ID,
            Money(BigDecimal.valueOf(140), Currency.EUR),
            InvoiceStatus.INCONSISTENT_DATA
        )
        val invoice =
            Invoice(111, NON_EXISTENT_CUSTOMER_ID, Money(BigDecimal.valueOf(140), Currency.EUR), InvoiceStatus.PENDING)

        every { dal.updateInvoice(expected) } returns expected

        assertEquals(expected, billingService.charge(invoice))
        verify { dal.updateInvoice(expected) }

    }

    @Test
    fun `If the invoice currency does not match, currency must be converted and then attempt to charge it`() {
        val expected = Invoice(
            111,
            EXISTENT_CUSTOMER_ID,
            Money(BigDecimal.valueOf(140), Currency.EUR),
            InvoiceStatus.PAID
        )

        every {
            currencyConverter.convert(
                Money(BigDecimal.valueOf(1041.78), Currency.DKK),
                Currency.EUR
            )
        } returns Money(
            BigDecimal.valueOf(140), Currency.EUR
        )

        every {
            paymentProvider.charge(
                Invoice(
                    111,
                    EXISTENT_CUSTOMER_ID,
                    Money(BigDecimal.valueOf(140), Currency.EUR),
                    InvoiceStatus.PENDING
                )
            )
        } returns true

        every { dal.updateInvoice(expected) } returns expected

        val invoice =
            Invoice(111, EXISTENT_CUSTOMER_ID, Money(BigDecimal.valueOf(1041.78), Currency.DKK), InvoiceStatus.PENDING)

        assertEquals(expected, billingService.charge(invoice))
        verify { dal.updateInvoice(expected) }
    }

    @Test
    fun `If a network error occurs, a NetworkException should be thrown`() {
        val invoice =
            Invoice(111, NON_EXISTENT_CUSTOMER_ID, Money(BigDecimal.valueOf(140), Currency.EUR), InvoiceStatus.PENDING)

        every { paymentProvider.charge(invoice) } throws NetworkException()

        assertThrows(NetworkException::class.java) { throw NetworkException() }
    }

}
