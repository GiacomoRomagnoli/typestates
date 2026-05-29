package semantic.model

data class JavaType internal constructor(
    val qualifiedName: String,
    val arrayLevel: Int
)
