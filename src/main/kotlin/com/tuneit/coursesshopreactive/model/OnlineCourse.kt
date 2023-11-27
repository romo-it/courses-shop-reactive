package com.tuneit.coursesshopreactive.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table(name = "online_courses")
data class OnlineCourse (
    @Id
    val id : Long?=null,
    val name : String,
    val price : Float,
    val author : String,
    val direction : String,
    val startDate : LocalDate,
    val endDate: LocalDate?=null,
)