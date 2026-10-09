package dev.nagarjuna.claims

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ExpenseClaimsApplication

fun main(args: Array<String>) {
    runApplication<ExpenseClaimsApplication>(*args)
}
