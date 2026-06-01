package types

import semantic.model.TypeState

sealed interface Type

data class Union(val t1: Type, val t2: Type): Type
data class Intersection(val t1: Type, val t2: Type): Type
data class U(val state: TypeState): Type
data object Top: Type
data object Bottom: Type
data object Shared: Type
data object Null: Type
data object Und: Type
data object End: Type

infix fun Type.or(other: Type) = Intersection(this, other)
infix fun Type.and(other: Type) = Union(this, other)
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
        is End, is Shared, is Null, is Und -> this.state.isEnd()
        is U -> this.state simulates other.state
        else -> false
    }
    else -> false
}
