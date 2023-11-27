package com.tuneit.coursesshopreactive.controller

import com.sletmoe.bucket4k.SuspendingBucket
import com.tuneit.coursesshopreactive.model.CourseRequest
import com.tuneit.coursesshopreactive.model.NotFoundException
import com.tuneit.coursesshopreactive.model.OnlineCourse
import com.tuneit.coursesshopreactive.service.CoursesService
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Refill

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@RestController
@RequestMapping("/courses")
class CoursesController(val service: CoursesService) {

    @GetMapping
    fun getAllCourses() : Flux<OnlineCourse> {
        return service.getAllCourses()
    }

    @GetMapping("/{id}")
    fun getCourseById(@PathVariable id:Long) : Mono<OnlineCourse> {
        return service.getCourseById(id)
    }

    @PostMapping
    fun saveCourse(@Valid @RequestBody request: CourseRequest) : Mono<OnlineCourse> {
        return service.saveCourse(request)
    }

    @PutMapping("/{id}")
    fun updateCourse(@PathVariable id:Long, @Valid @RequestBody request: CourseRequest) : Mono<OnlineCourse> {
       return service.updateCourse(id, request)
    }

    @DeleteMapping("/{id}")
    fun deleteCourse(@PathVariable id:Long) : Mono<String> {
       return service.deleteCourse(id)
                     .then(Mono.just("The course with id «$id» has been successfully deleted"))
    }
}