package language.types

import language.model.JavaEnum
import javax.lang.model.util.Types

data class EnumType(val enum: JavaEnum, private val typeUtils: Types) : Type {
    override fun sub(other: Type) = when (other) {
        is EnumType -> typeUtils.isSubtype(enum.element.asType(), other.enum.element.asType())
        is ClassType -> typeUtils.isSubtype(enum.element.asType(), other.clazz.element.asType())
        else -> false
    }
}
