package semantic

class SemanticException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)