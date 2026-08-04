package language.model

import annotations.Ensures
import protocol.model.JavaType
import protocol.model.Method
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class JavaMethod(
    override val element: ExecutableElement,
    program: Program
) : JavaExecutable(element, program) {

    private fun TypeMirror.arrayLevel(): Int {
        var current = this
        var arrayLevel = 0
        while (current.kind == TypeKind.ARRAY) {
            arrayLevel++
            current = (current as ArrayType).componentType
        }
        return arrayLevel
    }

    infix fun overrides(other: JavaMethod) =
        program.elements.overrides(
            element,
            other.element,
            element.enclosingElement as TypeElement
        )

    val pSig by lazy {
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
        val annotation = element.getAnnotation(Ensures::class.java)?.value?.toSet()
        element.returnType.toRT(annotation)
    }
}