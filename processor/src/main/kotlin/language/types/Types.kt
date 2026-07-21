package language.types

import language.model.ClassRef
import language.model.JavaClass
import language.model.JavaEnum
import language.model.at
import language.model.isSubClassOf

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
data object BoolUnd : TC

data object Integer : RT, TC
data object IntegerUnd : TC

data object Double : RT, TC
data object DoubleUnd : TC

data object Void : RT, TC

data object BottomTC : TC

data class EnumType(val enum: JavaEnum, val und: Boolean = false) : PT, TC

data class ErrorType(val message: String) : PT

data class ClassType(val clazz: ClassRef, val type: T): PT {
    val isWellFormed by lazy {
        if (clazz.isLinear)
            typestates(type).all { it in clazz.protocol!!.protIn }
        else
            typestates(type).isEmpty()
    }
}

infix fun ClassType.sub(other: ClassType) =
    clazz isSubClassOf other.clazz && this.isWellFormed && other.isWellFormed && this.type sub other.type

fun term(tc: TC) = if (tc is TypeStateTree) tc.classType sub (tc.clazz at Und) else true

fun mergeTC(tc1: TC, tc2: TC): TC = when {
    tc1 is TypeStateTree && tc2 is TypeStateTree -> mergeTT(tc1, tc2)
    tc1 sub tc2 -> tc2
    tc2 sub tc1 -> tc1
    else -> error("mergeTC is undefined for parameters (${tc1.javaClass.simpleName}, ${tc2.javaClass.simpleName})")
}

infix fun TC.sub(other: TC) = when(this) {
    is BottomTC -> true
    is EnumType -> other is EnumType && enum == other.enum && (und == other.und || !und)
    is TypeStateTree -> other is TypeStateTree && this sub other
    is Bool -> other is Bool || other is BoolUnd
    is Integer -> other is Double || other is DoubleUnd || other is Integer || other is IntegerUnd
    is Double -> other is Double || other is DoubleUnd
    is BoolUnd -> other is BoolUnd
    is DoubleUnd -> other is DoubleUnd
    is IntegerUnd -> other is IntegerUnd
    is Void -> other is Void
}

