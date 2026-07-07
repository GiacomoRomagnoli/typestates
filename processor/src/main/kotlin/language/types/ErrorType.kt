package language.types

data class ErrorType(val reason: String) : Type {
    override fun sub(other: Type): Boolean = false
}
