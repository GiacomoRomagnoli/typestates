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

infix fun Type.or(other: Type) = Intersection(this, other)
infix fun Type.and(other: Type) = Union(this, other)