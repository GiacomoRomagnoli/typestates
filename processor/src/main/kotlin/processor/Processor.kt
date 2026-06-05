package processor

import processor.environment.Java
import processor.environment.classOf
import processor.environment.linearTypes
import types.chkProt
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class Processor: AbstractProcessor() {

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        Java.environment(processingEnv)
    }

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(
        Ensures::class.java.canonicalName,
        Requires::class.java.canonicalName,
        Typestate::class.java.canonicalName,
    )

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.linearTypes().forEach {
            val c = Java.env.classOf(it)
            chkProt(c)
        }
        return true
    }
}