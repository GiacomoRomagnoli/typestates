package semantic.model

data class Method internal constructor(
    val simpleName: String,
    val args: List<JavaType>
)
