package protocol.model

data class Method(
    val simpleName: String,
    val args: List<JavaType>
)
