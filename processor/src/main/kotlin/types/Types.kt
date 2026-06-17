package types

import classes.LinearClass
import classes.protIn
import semantic.model.Method
import semantic.model.OutPutState
import semantic.model.TypeState
import semantic.model.State

sealed interface Type

data class Union(val t1: Type, val t2: Type): Type
data class Intersection(val t1: Type, val t2: Type): Type
data class O(val state: OutPutState): Type
data class U(val state: TypeState): Type
data object Top: Type
data object Bottom: Type
data object Shared: Type
data object Null: Type
data object Und: Type
data object End: Type

infix fun Type.or(other: Type) = Intersection(this, other)
infix fun Type.and(other: Type) = Union(this, other)
val Type.labels: Set<String>
    get() = when (this) {
        is Union -> t1.labels + t2.labels
        is Intersection -> t1.labels + t2.labels
        is O -> state.labels
        else -> emptySet()
    }
val Type.isResolved : Boolean
    get() = when (this) {
        is Union -> t1.isResolved && t2.isResolved
        is Intersection -> t1.isResolved && t2.isResolved
        is O -> false
        else -> true
    }
infix fun Type.sub(other: Type): Boolean = when {
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
        is End, is Shared, is Null, is Und -> this.state.isEnd
        is U -> this.state simulates other.state
        else -> false
    }
    else -> false
}
fun typestates(t: Type): Set<State> = when(t) {
    is Union -> typestates(t.t1) + typestates(t.t2)
    is Intersection -> typestates(t.t1) + typestates(t.t2)
    is U -> setOf(t.state)
    is O -> setOf(t.state)
    else -> emptySet()
}
fun ucast(t: Type, c1: LinearClass, c2: LinearClass): Type = when(t) {
    is Union -> ucast(t.t1, c1, c2) and ucast(t.t2, c1, c2)
    is Intersection -> ucast(t.t1, c1, c2) or ucast(t.t2, c1, c2)
    is U -> protIn(c2).map { U(it) as Type }.filter { t sub it }.reduceOrNull { t1, t2 -> t1 or t2 } ?: Top
    else -> t
}
fun dcast(t: Type, c1: LinearClass, c2: LinearClass): Type = when(t) {
    is Union -> dcast(t.t1, c1, c2) and dcast(t.t2, c1, c2)
    is Intersection -> dcast(t.t1, c1, c2) or dcast(t.t2, c1, c2)
    is U -> protIn(c2).map { U(it) as Type }.filter { it sub t }.reduceOrNull { t1, t2 -> t1 and t2 } ?: Bottom
    else -> t
}
fun evoI(t: Type, mt: Method): Type = when(t) {
    is Union -> evoI(t.t1, mt) and evoI(t.t2, mt)
    is Intersection -> evoI(t.t1, mt) or evoI(t.t2, mt)
    is U -> when(val w = t.state[mt]) {
        is OutPutState -> O(w)
        is TypeState -> U(w)
        null -> Top
    }
    else -> Top
}
fun evoO(t: Type, l: String): Type = when(t) {
    is Union -> evoO(t.t1, l) and evoO(t.t2, l)
    is Intersection -> evoO(t.t1, l) or evoO(t.t2, l)
    is O -> when(val u = t.state[l]) {
        is TypeState -> U(u)
        null -> t
    }
    else -> t
}
fun resolve(t: Type): Type = when(t) {
    is Union -> resolve(t.t1) and resolve(t.t2)
    is Intersection -> resolve(t.t1) or resolve(t.t2)
    is O -> t.state.typeStates.map { U(it) as Type }.reduceOrNull { t1, t2 -> t1 and t2 } ?: Top
    else -> t
}