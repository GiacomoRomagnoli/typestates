package language.types

sealed interface Type {
    infix fun sub(other: Type): Boolean
}