package language.model

import language.types.ClassType
import language.types.T
import protocol.model.Protocol
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class JavaClass(
    val element: TypeElement,
    private val declaredProtocol: Protocol?,
    private val program: Program,
    private val ctx: JavaModelContext
) {
    infix fun isSubClassOf(other: JavaClass) = ctx.types.isSubtype(element.asType(), other.element.asType())
    infix fun at(t: T) = ClassType(this, t)
    val superclass by lazy {
        (ctx.types.asElement(element.superclass) as? TypeElement)?.let { program[it.qualifiedName.toString()] }
    }
    val meths by lazy {
        element.enclosedElements
            .filter { it.kind == ElementKind.METHOD }
            .map { JavaMethod(it as ExecutableElement, program, ctx) }
    }
    val allMeths: List<JavaMethod> by lazy {
        if (superclass == null) meths else meths + superclass!!.allMeths
    }
    val protocol: Protocol? by lazy {
        declaredProtocol ?: superclass?.protocol
    }
    val qualifiedName = element.qualifiedName.toString()
    val isLinear get() = protocol != null
}