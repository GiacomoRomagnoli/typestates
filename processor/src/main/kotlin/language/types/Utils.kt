package language.types

import language.model.JavaClass
import language.model.JavaEnum
import language.model.Program
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

fun TypeMirror.arrayLevel(): Int {
    var current = this
    var arrayLevel = 0
    while (current.kind == TypeKind.ARRAY) {
        arrayLevel++
        current = (current as ArrayType).componentType
    }
    return arrayLevel
}

fun TypeMirror.resolve(typeUtils: Types, elementUtils: Elements, annotation: Array<String>?, program: Program) =
    when (this.kind) {
        TypeKind.BOOLEAN ->  Bool
        TypeKind.INT -> Integer
        TypeKind.DOUBLE ->  Double
        TypeKind.VOID ->  Void
        TypeKind.DECLARED -> {
            val typeElement = typeUtils.asElement(this) as TypeElement
            val javaClass = program[typeElement.qualifiedName.toString()]
                ?: JavaClass(typeElement, null, program, typeUtils, elementUtils)
            when (typeElement.kind) {
                ElementKind.CLASS -> annotation
                    ?.let { value ->
                        val type = value.map { javaClass.protocol?.get(it) }
                        if (type.any { it == null }) {
                            ErrorType("invalid typestate")
                        } else {
                            ClassType(
                                javaClass,
                                type.map { U(it!!) as T }.reduceOrNull { t1, t2 -> t1 and t2 }
                                    ?: return@let ErrorType("empty type")
                            )
                        }
                    } ?: ClassType(javaClass, Null)
                ElementKind.ENUM -> EnumType(JavaEnum(typeElement))
                else -> ErrorType("not implemented type")
            }
        }
        else -> ErrorType("not implemented type")
    }