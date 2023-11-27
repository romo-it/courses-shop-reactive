package com.tuneit.coursesshopreactive.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebExchange
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Comparator
import java.util.stream.Collectors

@RestControllerAdvice
class RestControllerExceptionHandler {

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleWebExchangeBindException(e: WebExchangeBindException, we: ServerWebExchange): ResponseEntity<ExceptionDetails> {
        val request = we.request
        val errors = e.bindingResult
            .allErrors
            .stream()
            .map { it.defaultMessage?:"" }
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.toSet())
        val details = ExceptionDetails(
            path = request.path.value(),
            status = e.statusCode.value(),
            error =  e.reason?:HttpStatus.BAD_REQUEST.reasonPhrase,
            message = errors.toString().replace("\\[*]*".toRegex(), ""),
            requestId = request.id
        )
        return ResponseEntity.status(e.statusCode).body(details)
    }

    data class ExceptionDetails (
        val timestamp: String = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS+00:00")
                                                  .withZone(ZoneId.from(ZoneOffset.UTC)).format(Instant.now()),
        val path: String="",
        val status: Int,
        val error: String="",
        val message: String="",
        val requestId: String=""
    )
}
