package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Money

/**
 * External service which handles currency conversion. Assuming it never fails for brevity
 */
interface CurrencyConverter {
    fun convert(money: Money, currency: Currency): Money
}