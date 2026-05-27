package dsl

import processor.environment.allMeths
import processor.environment.allRt
import processor.environment.match
import processor.environment.protocol
import semantic.model.OutPutState
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class CheckerScope(val context: ProcessingEnvironment) {

    fun chkProt(clazz: TypeElement): Boolean {
        val protocol = context.protocol(clazz)
        val classMethods = context.allMeths(clazz)
        var holds = true
        for (s in protocol.model.protIn()) {
            for (m in s.methods()) {
                val method = classMethods.find { context.match(it, m) }
                if (method == null) {
                    holds = false
                    context.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "method ${m.simpleName} at "
                                + "${protocol.nodeOf(m)?.position?.startLineCol} in "
                                + "${protocol.ast.position.filename} has not been declared"
                    )
                } else when (val w = s[m]) {
                    is OutPutState -> {
                        val expected = context.allRt(method)
                        val actual = w.labels()
                        if (expected != actual) {
                            holds = false
                            context.messager.printMessage(
                                Diagnostic.Kind.ERROR,
                                "output state at ${protocol.nodeOf(w)?.position?.startLineCol} in "
                                        + "${protocol.ast.position.filename} is not exhaustive. "
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

    companion object {
        fun check(context: ProcessingEnvironment, block: CheckerScope.() -> Unit) =
            CheckerScope(context).block()
    }
}