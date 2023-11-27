package com.tuneit.coursesshopreactive.repository

import com.tuneit.coursesshopreactive.model.OnlineCourse
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CoursesRepository : ReactiveCrudRepository<OnlineCourse, Long> {
}