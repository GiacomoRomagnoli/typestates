package language.types

import language.model.JavaClass
import language.model.JavaMethod
import protocol.model.OutPutState
import protocol.model.TypeState
import protocol.model.State

sealed interface T

data class Union(val t1: T, val t2: T): T
data class Intersection(val t1: T, val t2: T): T
data class O(val state: OutPutState): T
data class U(val state: TypeState): T
data object Top: T
data object Bottom: T
data object Shared: T
data object Null: T
data object Und: T

infix fun T.or(other: T) = Intersection(this, other)
infix fun T.and(other: T) = Union(this, other)
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
val T.isTerminated : Boolean
    get() = when (this) {
        is Union -> t1.isTerminated && t2.isTerminated
        is Intersection -> t1.isTerminated || t2.isTerminated
        is U -> state.isEnd || state.isDroppable
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
        is Shared, is Null, is Und -> this.state.isEnd
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
fun ucast(t: T, c1: JavaClass, c2: JavaClass): T = when(t) {
    is Union -> ucast(t.t1, c1, c2) and ucast(t.t2, c1, c2)
    is Intersection -> ucast(t.t1, c1, c2) or ucast(t.t2, c1, c2)
    is U -> c2.protocol!!.protIn.map { U(it) as T }.filter { t sub it }.reduceOrNull { t1, t2 -> t1 or t2 } ?: Top
    else -> t
}
fun dcast(t: T, c1: JavaClass, c2: JavaClass): T = when(t) {
    is Union -> dcast(t.t1, c1, c2) and dcast(t.t2, c1, c2)
    is Intersection -> dcast(t.t1, c1, c2) or dcast(t.t2, c1, c2)
    is U -> c2.protocol!!.protIn.map { U(it) as T }.filter { it sub t }.reduceOrNull { t1, t2 -> t1 and t2 } ?: Bottom
    else -> t
}
fun evoI(t: T, mt: JavaMethod): T = when(t) {
    is Union -> evoI(t.t1, mt) and evoI(t.t2, mt)
    is Intersection -> evoI(t.t1, mt) or evoI(t.t2, mt)
    is U -> when(val w = t.state[mt.sign]) {
        is OutPutState -> O(w)
        is TypeState -> U(w)
        null -> t
    }
    else -> Top
}
fun evoO(t: T, l: String): T = when(t) {
    is Union -> evoO(t.t1, l) and evoO(t.t2, l)
    is Intersection -> evoO(t.t1, l) or evoO(t.t2, l)
    is O -> when(val u = t.state[l]) {
        is TypeState -> U(u)
        null -> t
    }
    else -> t
}
fun resolve(t: T): T = when(t) {
    is Union -> resolve(t.t1) and resolve(t.t2)
    is Intersection -> resolve(t.t1) or resolve(t.t2)
    is O -> t.state.typeStates.map { U(it) as T }.reduceOrNull { t1, t2 -> t1 and t2 } ?: Top
    else -> t
}