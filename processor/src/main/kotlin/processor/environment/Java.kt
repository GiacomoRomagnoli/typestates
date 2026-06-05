package processor.environment

import javax.annotation.processing.ProcessingEnvironment

object Java {
    private var environment: ProcessingEnvironment? = null
    fun environment(processingEnvironment: ProcessingEnvironment) { environment = processingEnvironment }
    val env get() = environment ?: error("processing environment not initialized")
    val filer get() = environment?.filer ?: error("processing environment not initialized")
    val types get() = environment?.typeUtils ?: error("processing environment not initialized")
    val elements get() = environment?.elementUtils ?: error("processing environment not initialized")
    val messager get() = environment?.messager ?: error("processing environment not initialized")
}