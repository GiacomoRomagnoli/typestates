package language.model

import annotations.Ensures
import annotations.Requires
import language.types.PT
import language.types.arrayLevel
import language.types.resolve
import protocol.model.JavaType
import protocol.model.Method
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class JavaMethod(
    val element: ExecutableElement,
    private val program: Program,
    private val ctx: JavaModelContext
) {
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
        element.returnType.resolve(ctx.types, ctx.elements, annotation, program)
    }

    val pt by lazy {
        element.parameters.map { parameter ->
            val annotation = parameter.getAnnotation(Requires::class.java)?.value
            parameter.asType().resolve(ctx.types, ctx.elements, annotation, program) as PT
        }
    }
}