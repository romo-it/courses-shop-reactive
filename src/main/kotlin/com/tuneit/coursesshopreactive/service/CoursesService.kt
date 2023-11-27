package com.tuneit.coursesshopreactive.service

import com.tuneit.coursesshopreactive.model.CourseRequest
import com.tuneit.coursesshopreactive.model.NotFoundException
import com.tuneit.coursesshopreactive.model.OnlineCourse
import com.tuneit.coursesshopreactive.repository.CoursesRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class CoursesService(val repo: CoursesRepository) {
    fun getAllCourses() : Flux<OnlineCourse> {
       return repo.findAll()
    }

    fun getCourseById(id:Long): Mono<OnlineCourse> {
       return repo.findById(id)
                  .switchIfEmpty(Mono.error(NotFoundException("The course with id «$id» isn't found")))
    }

   fun saveCourse(request: CourseRequest) : Mono<OnlineCourse> {
       return repo.save(
           OnlineCourse(
               name = request.name.trim(),
               price = request.price,
               author = request.author.trim(),
               direction = request.direction.trim(),
               startDate = request.startDate,
               endDate = request.endDate)
       )
   }

    fun updateCourse (id: Long, request: CourseRequest) : Mono<OnlineCourse> {
        return getCourseById(id)
               .flatMap {
                 repo.save(
                   OnlineCourse(
                    id = id,
                    name = request.name.trim(),
                    price = request.price,
                    author = request.author.trim(),
                    direction = request.direction.trim(),
                    startDate = request.startDate,
                    endDate = request.endDate)
                 )
               }
    }

    fun deleteCourse (id: Long): Mono<Void> {
         return getCourseById(id).flatMap { repo.deleteById(id) }
    }
}