package processor.elements

import javax.lang.model.element.ExecutableElement

val ExecutableElement.ensures
    get() = this.getAnnotation(Ensures::class.java)?.value ?: emptyArray()
