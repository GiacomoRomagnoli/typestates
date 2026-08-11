package language.model

import javax.lang.model.element.VariableElement

class JavaField(
    val element: VariableElement,
    private val program: Program
) {
    val name = element.simpleName.toString()
    val statement by lazy {
        program.trees.getPath(element) ?: return@lazy null
    }
}