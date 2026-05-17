package dsl

import ast.DecisionTargetNode
import ast.MethodNode
import ast.TypeNode
import semantic.Protocol.labels
import semantic.Protocol.parse
import semantic.Protocol.protIn
import semantic.Protocol.validate
import semantic.SemanticException
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.NoType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import javax.tools.StandardLocation

class CheckerScope(val context: ProcessingEnvironment) {

    fun TypeElement.allMethods() : List<ExecutableElement> {
        val methods = enclosedElements.filter { it.kind == ElementKind.METHOD }.map { it as ExecutableElement }
        return when (superclass) {
            is NoType -> methods
            else -> methods + (context.typeUtils.asElement(superclass) as TypeElement).allMethods()
                .filterNot { sm -> methods.any { context.elementUtils.overrides(it, sm, this) } }
        }
    }

    fun TypeNode.toJavaType(): TypeMirror {
        val qualified = name.joinToString(".") { it.value }
        var type = when (qualified) {
            "byte" -> context.typeUtils.getPrimitiveType(TypeKind.BYTE)
            "short" -> context.typeUtils.getPrimitiveType(TypeKind.SHORT)
            "int" -> context.typeUtils.getPrimitiveType(TypeKind.INT)
            "long" -> context.typeUtils.getPrimitiveType(TypeKind.LONG)
            "float" -> context.typeUtils.getPrimitiveType(TypeKind.FLOAT)
            "double" -> context.typeUtils.getPrimitiveType(TypeKind.DOUBLE)
            "boolean" -> context.typeUtils.getPrimitiveType(TypeKind.BOOLEAN)
            "char" -> context.typeUtils.getPrimitiveType(TypeKind.CHAR)
            else -> context.elementUtils.getTypeElement(qualified)?.asType()
        } ?: throw SemanticException(
            "type $qualified at ${position.startLineCol} " +
            "in ${position.filename} is not a valid Java type"
        )
        repeat(arrayLevel) {
            type = context.typeUtils.getArrayType(type)
        }
        return type
    }

    infix fun ExecutableElement.match(protocolMethod: MethodNode): Boolean {
        if (!simpleName.contentEquals(protocolMethod.name.value))
            return false
        if (parameters.size != protocolMethod.args.size)
            return false
        return parameters
            .map { it.asType() }
            .zip(protocolMethod.args)
            .all { (jt, pt) -> context.typeUtils.isSameType(jt, pt.toJavaType()) }
    }

    fun TypeElement.allRt(m: ExecutableElement): Set<String>? =
        when (m.returnType.kind) {
            TypeKind.BOOLEAN -> setOf("true", "false")
            TypeKind.DECLARED -> when (context.typeUtils.asElement(m.returnType).kind) {
                ElementKind.ENUM -> context.typeUtils.asElement(m.returnType).enclosedElements
                    .filter { it.kind == ElementKind.ENUM_CONSTANT }
                    .map { it.simpleName.toString() }
                    .toSet()
                else -> null
            }
            else -> null
        }

    fun chkProt(clazz: TypeElement) {
        val path = clazz.getAnnotation(Typestate::class.java).value
        val ps = context.filer.getResource(StandardLocation.CLASS_PATH, "", path).getCharContent(false).toString()
        val protocol = parse(ps)
        protocol.validate()
        val classMethods = clazz.allMethods()
        for (transition in protocol.protIn().flatMap { it.transitions }) {
                val method = classMethods.find { it match transition.method }
                if (method == null) {
                    context.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "method ${transition.method.name.value} at " +
                        "${transition.method.position.startLineCol} in " +
                        "${protocol.position.filename} has not been declared"
                    )
                } else when (val w = transition.target) {
                    is DecisionTargetNode -> {
                        val expected = clazz.allRt(method)
                        val actual = w.labels()
                        if (expected != actual) {
                            context.messager.printMessage(
                                Diagnostic.Kind.ERROR,
                                "output state at ${w.position.startLineCol} in " +
                                "${protocol.position.filename} is not exhaustive. " +
                                "expected=$expected actual=$actual"
                            )
                        }
                    }
                    else -> Unit
                }
        }
    }

    companion object {
        fun check(context: ProcessingEnvironment, block: CheckerScope.() -> Unit) =
            CheckerScope(context).block()
    }
}