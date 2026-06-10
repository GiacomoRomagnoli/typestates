package types

import SemanticModel
import processor.environment.Java
import processor.environment.allMeths
import processor.environment.allRt
import processor.environment.match
import semantic.model.OutPutState
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

data class LinearClass(val element: TypeElement, val semantic: SemanticModel) {
    val protocol get() = semantic.model
    val qualifiedName get() = element.qualifiedName.toString()
    val simpleName get() = element.simpleName.toString()
}

fun protIn(c: LinearClass) = c.protocol.protIn()
fun allMeths(c: LinearClass) = Java.env.allMeths(c.element)
fun chkProt(c: LinearClass): Boolean {
    val javaMethods = allMeths(c)
    var holds = true
    for (s in protIn(c)) {
        for (m in s.methods()) {
            val method = javaMethods.find { Java.env.match(it, m) }
            if (method == null) {
                holds = false
                Java.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "method ${m.simpleName} at "
                            + "${c.semantic.nodeOf(m)?.position?.startLineCol} in "
                            + "${c.semantic.ast.position.filename} has not been declared"
                )
            } else when (val w = s[m]) {
                is OutPutState -> {
                    val expected = Java.env.allRt(method)
                    val actual = w.labels()
                    if (expected != actual) {
                        holds = false
                        Java.messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "output state at ${c.semantic.nodeOf(w)?.position?.startLineCol} in "
                                    + "${c.semantic.ast.position.filename} is not exhaustive. "
                                    + "expected=$expected actual=$actual"
                        )
                    }
                }
                else -> Unit
            }
        }
    }
    return holds
}