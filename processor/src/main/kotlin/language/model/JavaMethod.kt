package language.model

import annotations.Ensures
import language.types.arrayLevel
import language.types.resolve
import protocol.model.JavaType
import protocol.model.Method
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class JavaMethod(
    override val element: ExecutableElement,
    program: Program,
    ctx: JavaModelContext
) : JavaExecutable(element, program, ctx) {
    infix fun overrides(other: JavaMethod) =
        ctx.elements.overrides(
            element,
            other.element,
            element.enclosingElement as TypeElement
        )
    val sign by lazy {
        Method(
            element.simpleName.toString(),
            element.parameters.map { it.asType() }.map {
                JavaType(
                    it.toString().replace("[]", "").substringBefore("<"),
                    it.arrayLevel()
                )
            }
        )
    }
    val rt by lazy {
        val annotation = element.getAnnotation(Ensures::class.java)?.value
        element.returnType.resolve(ctx, annotation, program)
    }
}