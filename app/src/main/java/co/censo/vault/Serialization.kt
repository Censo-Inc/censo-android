package co.censo.vault

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule


val jsonMapper = ObjectMapper().apply {
    registerModule(
        kotlinModule {
            configure(KotlinFeature.SingletonSupport, true)
        }
    )
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    registerModule(JavaTimeModule())
}