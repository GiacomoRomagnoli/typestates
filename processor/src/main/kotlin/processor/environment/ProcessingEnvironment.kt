package processor.environment

import SemanticModel
import ast.parse
import semantic.analyse
import semantic.model.JavaType
import semantic.model.Method
import types.Class
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.NoType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import javax.tools.StandardLocation
import kotlin.collections.plus

fun ProcessingEnvironment.classOf(element: TypeElement) : Class {
    val path = element.getAnnotation(Typestate::class.java)?.value
    val ps = filer.getResource(
        StandardLocation.CLASS_PATH,
        "",
        path
    ).getCharContent(false).toString()
    val ast = parse(ps)
    ast.analyse().forEach { messager.printMessage(Diagnostic.Kind.ERROR, it.message, element) }
    return Class(element, SemanticModel(ast))
}

fun ProcessingEnvironment.allMeths(clazz: TypeElement) : List<ExecutableElement> {
    val methods = clazz.enclosedElements.filter { it.kind == ElementKind.METHOD }.map { it as ExecutableElement }
    return when (clazz.superclass) {
        is NoType -> methods
        else -> methods + allMeths((typeUtils.asElement(clazz.superclass) as TypeElement))
            .filterNot { sm -> methods.any { elementUtils.overrides(it, sm, clazz) } }
    }
}

fun ProcessingEnvironment.toJavaType(jt: JavaType): TypeMirror? {
    var type = when (jt.qualifiedName) {
        "byte" -> typeUtils.getPrimitiveType(TypeKind.BYTE)
        "short" -> typeUtils.getPrimitiveType(TypeKind.SHORT)
        "int" -> typeUtils.getPrimitiveType(TypeKind.INT)
        "long" -> typeUtils.getPrimitiveType(TypeKind.LONG)
        "float" -> typeUtils.getPrimitiveType(TypeKind.FLOAT)
        "double" -> typeUtils.getPrimitiveType(TypeKind.DOUBLE)
        "boolean" -> typeUtils.getPrimitiveType(TypeKind.BOOLEAN)
        "char" -> typeUtils.getPrimitiveType(TypeKind.CHAR)
        else -> elementUtils.getTypeElement(jt.qualifiedName)?.asType()
    }
    repeat(jt.arrayLevel) {
        type = typeUtils.getArrayType(type)
    }
    return type
}

fun ProcessingEnvironment.match(jm: ExecutableElement, pm: Method): Boolean {
    if (!jm.simpleName.contentEquals(pm.simpleName))
        return false
    if (jm.parameters.size != pm.args.size)
        return false
    return jm.parameters
        .map { it.asType() }
        .zip(pm.args)
        .all { (jt, pt) -> typeUtils.isSameType(jt, toJavaType(pt)) }
}

fun ProcessingEnvironment.allRt(m: ExecutableElement): Set<String>? =
    when (m.returnType.kind) {
        TypeKind.BOOLEAN -> setOf("true", "false")
        TypeKind.DECLARED -> when (typeUtils.asElement(m.returnType).kind) {
            ElementKind.ENUM -> typeUtils.asElement(m.returnType).enclosedElements
                .filter { it.kind == ElementKind.ENUM_CONSTANT }
                .map { it.simpleName.toString() }
                .toSet()
            else -> null
        }
        else -> null
    }