package language.types

object PrimitiveTypes {
    object Boolean : Type {
        val labels = listOf("true", "false")
        override fun sub(other: Type) = other is Boolean
    }
    object Int : Type {
        override fun sub(other: Type) = other is Int || other is Double
    }

    object Double : Type {
        override fun sub(other: Type) = other is Double
    }

    object Void : Type {
        override fun sub(other: Type) = other is Void
    }
}