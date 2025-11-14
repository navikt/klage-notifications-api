package no.nav.klage.notifications.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.text.SimpleDateFormat

fun ourJacksonObjectMapper(): ObjectMapper {
    val jacksonObjectMapper = jacksonObjectMapper()
    jacksonObjectMapper.registerModule(JavaTimeModule())
    jacksonObjectMapper.dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    return jacksonObjectMapper
}