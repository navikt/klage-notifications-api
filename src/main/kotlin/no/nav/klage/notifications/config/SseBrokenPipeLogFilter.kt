package no.nav.klage.notifications.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.core.spi.FilterReply
import no.nav.klage.notifications.util.getLogger
import org.slf4j.Marker

class SseBrokenPipeLogFilter : TurboFilter() {

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
        if (throwable != null) {
            if (
                (throwable.javaClass.name == "java.io.IOException" &&
                 throwable.message == "Broken pipe" &&
                 logger?.name?.contains("org.apache.catalina.core.ContainerBase") == true
                )
                /*
                 ||
                (throwable.javaClass.name == "AsyncRequestNotUsableException" &&
                 throwable.message?.contains("Broken pipe", ignoreCase = true) == true
                )
                 */
            ) {
                ourLogger.debug("Suppressing error log message when broken pipe and logger is ${logger.name}. This is probably due to lost client during async/SSE operations.")
                return FilterReply.DENY
            }
        }

        if (level == Level.WARN && logger?.name == "org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver" && format?.contains("java.io.IOException: Broken pipe") == true) {
            ourLogger.debug("Suppressing warning log message when broken pipe and logger is ${logger.name}. This is probably due to lost client during async/SSE operations.")
            return FilterReply.DENY
        }

        if (level == Level.WARN && logger?.name == "no.nav.klage.config.problem.GlobalExceptionHandler" && format?.contains("Response already committed") == true) {
            ourLogger.debug("Suppressing warning log message when response already committed and logger is ${logger.name}. This is probably due to lost client during async/SSE operations.")
            return FilterReply.DENY
        }

        if (level == Level.WARN &&
            logger?.name == "org.eclipse.jetty.ee10.servlet.ServletChannel" &&
            throwable != null &&
            throwable.javaClass.name == "jakarta.servlet.ServletException" &&
            throwable.message?.contains("Request processing failed") == true &&
            format?.contains("events") == true
        ) {
            ourLogger.debug("Suppressing warning log message. This is probably due to lost client during async/SSE operations.")
            return FilterReply.DENY
        }

        if (level == Level.WARN &&
            logger?.name == "org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver" &&
            throwable != null &&
            throwable.javaClass.name == "java.lang.IllegalStateException" &&
            format?.contains("JwtTokenUnauthorizedException") == true
        ) {
            ourLogger.debug("Suppressing warning log message. This is probably due to lost client during async/SSE operations.")
            return FilterReply.DENY
        }

        return FilterReply.NEUTRAL
    }
}