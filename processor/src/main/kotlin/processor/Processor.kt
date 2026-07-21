package processor

import annotations.Ensures
import annotations.Requires
import annotations.Typestate
import ast.parse
import com.sun.source.util.Trees
import language.model.JavaClass
import language.model.JavaModelContext
import language.model.Program
import protocol.compile
import rules.chkOvr
import rules.chkProt
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

class Processor: AbstractProcessor() {
    val program = Program(processingEnv.elementUtils, Trees.instance(processingEnv))

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(
        Ensures::class.java.canonicalName,
        Requires::class.java.canonicalName,
        Typestate::class.java.canonicalName,
    )

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.classes.forEach { program.add(javaClassOf(it)) }
        for(clazz in program.allClasses) {
            if (clazz.isLinear)
                if(!chkProt(clazz))
                    emitError("chkProt failed for ${clazz.qualifiedName}")
            if (clazz.superclass != null)
                if(!chkOvr(clazz, clazz.superclass!!))
                    emitError("chkOvr failed for ${clazz.qualifiedName}")
        }
        return true
    }

    private val RoundEnvironment.classes
        get() = rootElements.filter { it.kind == ElementKind.CLASS }.map { it as TypeElement }

    private fun loadProtocol(clazz: TypeElement) =
        clazz.getAnnotation(Typestate::class.java)?.let { annotation ->
            val resource = processingEnv.filer.getResource(
                StandardLocation.CLASS_PATH,
                "",
                annotation.value
            ).getCharContent(false).toString()
            val compilation = compile(parse(resource))
            compilation.errors.forEach { emitError(it.message) }
            program.add(compilation.protocol, compilation.binding)
            compilation.protocol
        }

    private val ctx = JavaModelContext.from(processingEnv)

    private fun javaClassOf(element: TypeElement) =
        JavaClass(element,loadProtocol(element), program, ctx)

    private fun emitError(msg: String) =
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, msg)
}