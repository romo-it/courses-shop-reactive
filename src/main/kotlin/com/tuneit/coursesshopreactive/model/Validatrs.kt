package com.tuneit.coursesshopreactive.model

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.util.ObjectUtils
import java.util.*
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties


@Constraint(validatedBy = [RequiredByConditionValidator::class])
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@JvmRepeatable(
    RequiredByCondition.List::class
)
annotation class RequiredByCondition(
    val message: String = "must be specified",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
    val conditionalProperty: String,
    val triggerValues: Array<String>,
    val requiredProperties: Array<String>
) {
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    @kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
    @MustBeDocumented
    annotation class List(vararg val value: RequiredByCondition)
}

class RequiredByConditionValidator : ConstraintValidator<RequiredByCondition, Any?> {
    private var conditionalProperty: String? = null
    private lateinit var triggerValues: Array<String>
    private lateinit var requiredProperties: Array<String>
    private var message: String? = null
    override fun initialize(constraint: RequiredByCondition) {
        conditionalProperty = constraint.conditionalProperty
        triggerValues = constraint.triggerValues
        requiredProperties = constraint.requiredProperties
        message = constraint.message
    }

    override fun isValid(o: Any?, context: ConstraintValidatorContext): Boolean {
        try {
            val conditionalPropertyValue: Any? = o?.getField(conditionalProperty!!)
            if (isValidationNeeded(conditionalPropertyValue)) {
                return validateRequiredProperties(o, context)
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun isValidationNeeded(conditionalPropertyValue: Any?): Boolean {
        val values = listOf(*triggerValues)
        val isEmpty: Boolean = ObjectUtils.isEmpty(conditionalPropertyValue)
        return if (isEmpty && values.stream().anyMatch {
                    it: String ->
                it.equals("NULL", ignoreCase = true) || it.equals("EMPTY", ignoreCase = true) })
            true
        else values.contains(conditionalPropertyValue)
    }

    private fun validateRequiredProperties(o: Any?, context: ConstraintValidatorContext): Boolean {
        var isValid = true
        listOf(*requiredProperties).forEach(Consumer { it: String? ->
            val requiredPropertyValue: Any? = o?.getField(it!!)
            val isNotSpecified: Boolean = ObjectUtils.isEmpty(requiredPropertyValue)
            if (isNotSpecified) {
                isValid = false
                context.disableDefaultConstraintViolation()
                context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(it)
                    .addConstraintViolation()
            }
        })
        return isValid
    }

    @Throws(IllegalAccessException::class, ClassCastException::class)
    inline fun <reified T> Any.getField(fieldName: String): T? {
        this::class.memberProperties.forEach { kCallable ->
            if (fieldName == kCallable.name) {
                return kCallable.getter.call(this) as T?
            }
        }
        return null
    }
}