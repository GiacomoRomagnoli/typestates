package language.types

import language.model.ClassRef
import language.model.JavaClass
import language.model.JavaEnum
import language.model.at
import language.model.isSubClassOf

/**
 * interface that represents a java type
 */
sealed interface JT
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

data object Bool : PT, TC, JT {
    const val TRUE = "true"
    const val FALSE = "false"
    val labels = listOf(TRUE, FALSE)
}
data object BoolUnd : TC

data object Integer : PT, TC, JT
data object IntegerUnd : TC

data object Double : PT, TC, JT
data object DoubleUnd : TC

data object Void : RT, TC, JT

data object BottomTC : TC

data class EnumType(val enum: JavaEnum, val und: Boolean = false) : PT, TC, JT

data class JClass(val clazz: JavaClass) : JT

val JT.isLinear get() = this is JClass && clazz.isLinear

data class ClassType(val clazz: ClassRef, val type: T): PT {
    val isWellFormed by lazy {
        typestates(type).all { it in clazz.protocol?.protIn.orEmpty() }
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

fun alias(tc: TC) =
    if (tc is TypeStateTree)
        if (tc.type == Null)
            tc
        else if (tc.type.isResolved && tc sub tt(tc.clazz, Shared))
            tt(tc.clazz, Shared)
        else
            tt(tc.clazz, Top)
    else tc

fun toTC(rt: RT): TC =
    when (rt) {
        is ClassType -> tt(rt.clazz, rt.type)
        is TC -> rt
    }

fun toTC(jt: JT): TC =
    when (jt) {
        is JClass -> tt(jt.clazz, Null)
        is TC -> jt
    }

fun TC.defined() =
    when (this) {
        Bool, BoolUnd -> Bool
        Integer, IntegerUnd -> Integer
        Double, DoubleUnd -> Double
        is EnumType -> copy(und = false)
        else -> this
    }