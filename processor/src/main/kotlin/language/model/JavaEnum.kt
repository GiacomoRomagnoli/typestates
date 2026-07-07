package language.model

import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

class JavaEnum(val element: TypeElement) {
    val labels by lazy {
        element.enclosedElements
            .filter { it.kind == ElementKind.ENUM_CONSTANT }
            .map { it.simpleName.toString() }
    }
    val qualifiedName = element.qualifiedName.toString()
}