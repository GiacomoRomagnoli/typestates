package language.model

import language.types.resolve
import javax.lang.model.element.VariableElement

class JavaField(
    val element: VariableElement,
    private val program: Program,
    private val ctx: JavaModelContext,
) {
    val name = element.simpleName.toString()
    val type by lazy {
        element.asType().resolve(ctx, null, program)
    }
}