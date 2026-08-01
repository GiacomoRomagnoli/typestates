package language.model

import javax.lang.model.element.VariableElement

class JavaField(
    val element: VariableElement,
    private val program: Program,
    private val ctx: JavaModelContext,
) {
    val name = element.simpleName.toString()
    val jt: Nothing by lazy {
        TODO("must return a class an enum or a primitive type")
    }
}