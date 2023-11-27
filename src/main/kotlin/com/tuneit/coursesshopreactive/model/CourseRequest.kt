package com.tuneit.coursesshopreactive.model;

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDate

data class CourseRequest(
    @field:NotBlank(message = "name should be specified") val name : String,
    @field:PositiveOrZero(message = "price must be >= 0") val price : Float,
    @field:NotBlank(message = "author should be specified") val author : String,
    @field:NotBlank(message = "direction should be specified") val direction : String,
    val startDate : LocalDate,
    val endDate: LocalDate?=null
)
