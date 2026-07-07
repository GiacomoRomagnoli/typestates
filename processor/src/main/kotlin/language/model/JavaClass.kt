package language.model

import protocol.model.Protocol
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class JavaClass(
    val element: TypeElement,
    private val declaredProtocol: Protocol?,
    private val program: Program,
    private val typeUtils: Types,
    private val elementUtils: Elements,
) {
    val superclass by lazy {
        (typeUtils.asElement(element.superclass) as? TypeElement)?.let { program[it.qualifiedName.toString()] }
    }
    val meths by lazy {
        element.enclosedElements
            .filter { it.kind == ElementKind.METHOD }
            .map { JavaMethod(it as ExecutableElement, program, typeUtils, elementUtils) }
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