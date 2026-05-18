package dsl

import ast.OutPutStateNode
import ast.MethodNode
import ast.JavaTypeNode
import processor.allMeths
import processor.allRt
import processor.match
import processor.protocol
import processor.toJavaType
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

    fun chkProt(clazz: TypeElement): Boolean {
        val protocol = context.protocol(clazz)
        val classMethods = context.allMeths(clazz)
        var holds = true
        for (transition in protocol.protIn().flatMap { it.transitions }) {
            val method = classMethods.find { context.match(it, transition.method) }
            if (method == null) {
                holds = false
                context.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "method ${transition.method.name.value} at "
                            + "${transition.method.position.startLineCol} in "
                            + "${protocol.position.filename} has not been declared"
                )
            } else when (val w = transition.target) {
                is OutPutStateNode -> {
                    val expected = context.allRt(method)
                    val actual = w.labels()
                    if (expected != actual) {
                        holds = false
                        context.messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "output state at ${w.position.startLineCol} in "
                                    + "${protocol.position.filename} is not exhaustive. "
                                    + "expected=$expected actual=$actual"
                        )
                    }
                }
                else -> Unit
            }
        }
        return holds
    }

    companion object {
        fun check(context: ProcessingEnvironment, block: CheckerScope.() -> Unit) =
            CheckerScope(context).block()
    }
}