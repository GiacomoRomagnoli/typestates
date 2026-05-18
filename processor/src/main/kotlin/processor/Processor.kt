package processor

import dsl.CheckerScope.Companion.check
import processor.environment.linearTypes
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class Processor: AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(
        Ensures::class.java.canonicalName,
        Requires::class.java.canonicalName,
        Typestate::class.java.canonicalName,
    )

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.linearTypes().forEach {
            check(processingEnv) {
                chkProt(it)
            }
        }
        return true
    }
}