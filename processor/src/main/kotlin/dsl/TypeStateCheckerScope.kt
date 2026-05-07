package dsl

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.NoType

class TypeStateCheckerScope(val java: ProcessingEnvironment) {
    fun TypeElement.allMethods() : List<ExecutableElement> {
        val methods = enclosedElements.filter { it.kind == ElementKind.METHOD }.map { it as ExecutableElement }
        return when (superclass) {
            is NoType -> methods
            else -> methods + (java.typeUtils.asElement(superclass) as TypeElement).allMethods()
                .filterNot { sm -> methods.any { java.elementUtils.overrides(it, sm, this) } }
        }
    }

    companion object {
        fun TypeCheck(java: ProcessingEnvironment, block: TypeStateCheckerScope.() -> Unit) =
            TypeStateCheckerScope(java).block()
    }
}