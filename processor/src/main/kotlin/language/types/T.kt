package language.types

import language.model.ClassRef
import language.model.JavaClass
import language.model.isSubClassOf
import protocol.model.Method
import protocol.model.OutPutState
import protocol.model.TypeState
import protocol.model.State

sealed interface T
data class Union(val t1: T, val t2: T): T
data class Intersection(val t1: T, val t2: T): T
data class O(
    val state: OutPutState,
    private val f: O.(String) -> T? = { label -> state[label]?.let { U(it) }}
): T {
    operator fun get(label: String) = f(label)
}
data class U(val state: TypeState): T {
    operator fun get(mt: Method) =
        when (val w = state[mt]) {
            is OutPutState -> O(w)
            is TypeState -> U(w)
            null -> null
        }
}
data object Top: T
data object Bottom: T
data object Shared: T
data object Null: T
data object Und: T

infix fun T.intersect(other: T) = Intersection(this, other)
infix fun T.union(other: T) = Union(this, other)

val T.labels: Set<String>
    get() = when (this) {
        is Union -> t1.labels + t2.labels
        is Intersection -> t1.labels + t2.labels
        is O -> state.labels
        else -> emptySet()
    }

val T.isResolved : Boolean
    get() = when (this) {
        is Union -> t1.isResolved && t2.isResolved
        is Intersection -> t1.isResolved && t2.isResolved
        is O -> false
        else -> true
    }

fun term(t: T): Boolean =
    when (t) {
        is Union -> term(t.t1) && term(t.t2)
        is Intersection -> term(t.t1) || term(t.t2)
        is U -> t.state.isEnd || t.state.isDroppable
        else -> false
    }

infix fun T.sub(other: T): Boolean = when {
    this is Bottom -> true
    other is Top -> true
    this is Union -> this.t1 sub other && this.t2 sub other
    this is Intersection -> this.t1 sub other || this.t2 sub other
    other is Union -> this sub other.t1 || this sub other.t2
    other is Intersection -> this sub other.t1 && this sub other.t2
    this is Shared -> other is Shared
    this is Null -> other is Null
    this is Und -> other is Und
    this is U -> when (other) {
        is Shared, is Null, is Und -> term(this)
        is U -> this.state simulates other.state
        else -> false
    }
    else -> false
}

fun typestates(t: T): Set<State> = when(t) {
    is Union -> typestates(t.t1) + typestates(t.t2)
    is Intersection -> typestates(t.t1) + typestates(t.t2)
    is U -> setOf(t.state)
    is O -> setOf(t.state)
    else -> emptySet()
}

fun ucast(t: T, c1: ClassRef, c2: JavaClass): T {
    require(t.isResolved)
    require(c1 isSubClassOf c2)
    require(typestates(t).all { it in c1.protocol?.protIn.orEmpty() })
    fun rec(t: T): T =
        when (t) {
            is Union -> rec(t.t1) union rec(t.t2)
            is Intersection -> rec(t.t1) intersect rec(t.t2)
            is U -> c2.protocol?.protIn.orEmpty()
                .map { U(it) as T }
                .filter { t sub it }
                .reduceOrNull { t1, t2 -> t1 intersect t2 } ?: Top
            else -> t
        }
    return rec(t)
}

fun dcast(t: T, c1: ClassRef, c2: ClassRef): T {
    require(t.isResolved)
    require(c2 isSubClassOf c1)
    require(typestates(t).all { it in c1.protocol?.protIn.orEmpty() })
    fun rec(t: T): T =
        when (t) {
            is Union -> rec(t.t1) union rec(t.t2)
            is Intersection -> rec(t.t1) intersect rec(t.t2)
            is U -> c2.protocol?.protIn.orEmpty()
                .map { U(it) as T }
                .filter { it sub t }
                .reduceOrNull { t1, t2 -> t1 union t2 } ?: Bottom
            else -> t
        }
    return rec(t)
}

fun evoI(t: T, mt: Method): T =
    when(t) {
        is Union -> evoI(t.t1, mt) union evoI(t.t2, mt)
        is Intersection -> evoI(t.t1, mt) intersect evoI(t.t2, mt)
        is U -> t[mt] ?: Top
        else -> Top
    }

fun evoO(t: T, l: String): T =
    when(t) {
        is Union -> evoO(t.t1, l) union evoO(t.t2, l)
        is Intersection -> evoO(t.t1, l) intersect evoO(t.t2, l)
        is O -> t[l] ?: t
        else -> t
    }

fun resolve(t: T): T = when(t) {
    is Union -> resolve(t.t1) union resolve(t.t2)
    is Intersection -> resolve(t.t1) intersect resolve(t.t2)
    is O -> t.state.typeStates.map { U(it) as T }.reduce { t1, t2 -> t1 union t2 }
    else -> t
}

fun invert(t: T): T =
    when(t) {
        is Union -> invert(t.t1) union invert(t.t2)
        is Intersection -> invert(t.t1) intersect invert(t.t2)
        is O ->
            if (t.labels.all { it in Bool.labels })
                t.copy {
                    when (it) {
                        "true" -> state["false"]
                        "false" -> state["true"]
                        else -> null
                    }?.let { u -> U(u) }
                }
            else t
        else -> t
    }

fun toPair(t: T, l: String): T =
    when(t) {
        is Union -> toPair(t.t1, l) union toPair(t.t2, l)
        is Intersection -> toPair(t.t1, l) intersect toPair(t.t2, l)
        is O ->
            if (t[l] != null)
                t.copy {
                    when(it) {
                        "true" -> U(state[l]!!)
                        "false" -> (state.typeStates - state[l]!!)
                            .map { u -> U(u) as T }
                            .reduce { t1, t2 -> t1 union t2 }
                        else -> null
                    }
                }
            else t
        else -> t
    }