package processor

import processor.environment.Java
import processor.environment.annotatedTypeElements
import processor.environment.linearClassOf
import processor.environment.linearTypes
import processor.classes.NonLinearClass
import processor.classes.chkOvr
import processor.classes.chkProt
import processor.classes.sup
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

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
            val c = Java.env.linearClassOf(it)
            Java.messager.printMessage(
                Diagnostic.Kind.NOTE,
                """
                    PROTOCOL:
                    class -> ${c.element.qualifiedName}
                    chkProt -> ${chkProt(c)}
                """.trimIndent()
            )
        }
        roundEnv.annotatedTypeElements().map {
            when (it.getAnnotation(Typestate::class.java)) {
                null -> NonLinearClass(it)
                else -> Java.env.linearClassOf(it)
            }
        }.forEach {
            val sup = sup(it)
            Java.messager.printMessage(
                Diagnostic.Kind.NOTE,
                """
                    OVERRIDE:
                    class -> ${it.element.qualifiedName}
                    super class -> ${sup.element.qualifiedName}
                    chkOvr -> ${chkOvr(it, sup)}
                """.trimIndent()
            )
        }
        return true
    }
}