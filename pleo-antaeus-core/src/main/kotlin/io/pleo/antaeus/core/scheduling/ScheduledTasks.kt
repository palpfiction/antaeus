package io.pleo.antaeus.core.scheduling

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.data.AntaeusDal
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.time.LocalDateTime

class ScheduledTasks(
    private val billingService: BillingService,
    private val dal: AntaeusDal,
    private val scheduler: Scheduler = StdSchedulerFactory().scheduler
) {

    fun start() {
        if (scheduler.isStarted) return
        scheduler.start()
        scheduleChargeInvoicesMonthlyJob()
    }

    private fun scheduleChargeInvoicesMonthlyJob() {
        scheduler.scheduleJob(
            JobBuilder
                .newJob()
                .ofType(ChargeInvoicesMonthlyJob::class.java)
                .withIdentity("charge_invoices_monthly_${LocalDateTime.now()}")
                .usingJobData(
                    JobDataMap(
                        mapOf("billingService" to billingService, "dal" to dal)
                    )
                )
                .build(),
            TriggerBuilder
                .newTrigger()
                .withIdentity("trigger_charge_invoices_monthly_${LocalDateTime.now()}")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 1 * ? *"))
                .build()
        )
    }

}