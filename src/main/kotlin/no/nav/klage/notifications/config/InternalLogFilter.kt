package no.nav.klage.notifications.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.core.spi.FilterReply
import no.nav.klage.notifications.util.getLogger
import org.slf4j.Marker

class InternalLogFilter : TurboFilter() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val ourLogger = getLogger(javaClass.enclosingClass)
    }

    override fun decide(
        marker: Marker?,
        logger: Logger?,
        level: Level?,
        format: String?,
        params: Array<out Any>?,
        throwable: Throwable?
    ): FilterReply {
        if (level == Level.DEBUG && logger?.name == "org.springframework.web.filter.CommonsRequestLoggingFilter" &&
            (format?.contains("request [GET /internal/prometheus") == true ||
                    format?.contains("request [GET /internal/health") == true)
        ) {
            return FilterReply.DENY
        }

        return FilterReply.NEUTRAL
    }
}