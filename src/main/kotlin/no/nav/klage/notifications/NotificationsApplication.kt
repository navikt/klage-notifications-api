package no.nav.klage.notifications

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NotificationsApplication

fun main(args: Array<String>) {
    runApplication<NotificationsApplication>(*args)
}