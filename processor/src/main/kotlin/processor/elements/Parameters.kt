package processor.elements

import javax.lang.model.element.VariableElement

val VariableElement.requires
    get() = this.getAnnotation(Requires::class.java)?.value ?: emptyArray()