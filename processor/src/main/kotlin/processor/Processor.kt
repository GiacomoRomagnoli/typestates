package processor

import annotations.Ensures
import annotations.Requires
import annotations.Typestate
import ast.parse
import language.model.JavaClass
import language.model.Program
import protocol.compile
import rules.MissingMethod
import rules.NonExhaustiveOutPutState
import rules.UnexpectedOutPutState
import rules.chkOvr
import rules.chkProt
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

class Processor: AbstractProcessor() {
    val program = Program()

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(
        Ensures::class.java.canonicalName,
        Requires::class.java.canonicalName,
        Typestate::class.java.canonicalName,
    )

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.classes.forEach { program.add(JavaClassOf(it)) }
        for(clazz in program.allClasses) {
            if (clazz.isLinear)
                chkProt(clazz).forEach { emitError(it.msg) }
            if (clazz.superclass != null)
                chkOvr(clazz, clazz.superclass!!).forEach { emitError(it.msg) }
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

    private fun JavaClassOf(element: TypeElement) =
        JavaClass(
            element,
            loadProtocol(element),
            program,
            processingEnv.typeUtils,
            processingEnv.elementUtils
        )

    private fun emitError(msg: String) = processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, msg)

    private val rules.Diagnostic.msg
        get() = when (this) {
            is MissingMethod ->
                "missing method ${this.method.simpleName}"
            is NonExhaustiveOutPutState ->
                "not exhaustive output state expected: ${this.labels} actual: ${this.outputState.labels}"
            is UnexpectedOutPutState ->
                "unexpected output state for method ${this.method.sign.simpleName}"
            else -> "chkOvr fails"
        }
}