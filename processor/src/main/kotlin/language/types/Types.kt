package language.types

import language.model.JavaClass
import language.model.JavaEnum

/**
 * interface that represents a type of the type checker
 */
sealed interface TC

/**
 * interface that represents a return type
 */
sealed interface RT

/**
 * interface that represents a parameter type
 */
sealed interface PT : RT

data object Bool : RT, TC { val labels = listOf("true", "false") }

data object Integer : RT, TC

data object Double : RT, TC

data object Void : RT, TC

data class EnumType(val enum: JavaEnum) : PT, TC

data class ErrorType(val message: String) : PT

data class ClassType(val clazz: JavaClass, val type: T): PT {
    val isWellFormed by lazy {
        if (clazz.isLinear)
            typestates(type).all { it in clazz.protocol!!.protIn }
        else
            typestates(type).isEmpty()
    }
}

infix fun ClassType.sub(other: ClassType) =
    clazz isSubClassOf other.clazz && this.isWellFormed && other.isWellFormed && this.type sub other.type
