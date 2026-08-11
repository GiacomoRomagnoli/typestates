package language.model

import language.types.Bool
import language.types.Double
import language.types.EnumType
import language.types.Integer
import language.types.JClass
import javax.lang.model.element.ElementKind
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class JavaField(val element: VariableElement, private val program: Program) {

    private fun TypeMirror.toJT() =
        when(this.kind) {
            TypeKind.BOOLEAN ->  Bool
            TypeKind.INT -> Integer
            TypeKind.DOUBLE -> Double
            TypeKind.DECLARED -> {
                val typeElement = program.types.asElement(this)
                when (typeElement.kind) {
                    ElementKind.ENUM -> EnumType(program.enumByElement(typeElement))
                    ElementKind.CLASS -> JClass(program.classByElement(typeElement))
                    else -> TODO("not supported type $this")
                }
            }
            else -> TODO("not supported type $this")
        }

    val name = element.simpleName.toString()

    val statement by lazy {
        program.trees.getPath(element) ?: return@lazy null
    }

    val jt by lazy {
        element.asType().toJT()
    }

    val owner by lazy {
        program.classByElement(element.enclosingElement)
    }
}