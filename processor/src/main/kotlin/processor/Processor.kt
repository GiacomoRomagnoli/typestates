package processor

import annotations.Ensures
import annotations.Requires
import annotations.Typestate
import ast.parse
import com.sun.source.util.Trees
import language.model.Program
import protocol.compile
import javax.annotation.processing.AbstractProcessor
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import javax.tools.StandardLocation

abstract class Processor: AbstractProcessor() {
    protected val program by lazy {
        Program(
            Trees.instance(processingEnv),
            processingEnv.elementUtils,
            processingEnv.typeUtils,
            ::load
        )
    }

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(
        Ensures::class.java.canonicalName,
        Requires::class.java.canonicalName,
        Typestate::class.java.canonicalName,
    )

    protected fun load(path: String) =
        processingEnv.filer.getResource(StandardLocation.CLASS_PATH, "", path)
        .getCharContent(false)
        .toString()
        .let(::parse)
        .let(::compile)
        .also { it.errors.forEach { error -> emit(error.message) } }
        .protocol


    protected fun emit(msg: String, element: Element? = null) =
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, msg, element)
}