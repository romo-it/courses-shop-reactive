package com.tuneit.coursesshopreactive.model

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
data class NotFoundException(val msg: String) : RuntimeException(msg)

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
data class RateLimitException(val msg: String) : RuntimeException(msg)