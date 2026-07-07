package rules

import language.model.JavaClass
import language.types.EnumType
import language.types.PrimitiveTypes
import protocol.model.OutPutState

data class Diagnostic(val message: String)

fun chkProt(clazz: JavaClass): List<Diagnostic> {
    require(clazz.isLinear)
    val diagnostics = mutableListOf<Diagnostic>()
    for (transition in clazz.protocol!!.transitions) {
        val jmt = clazz.allMeths.find { it.sign == transition.method }
        if (jmt != null) {
            val state = transition.state
            if (state is OutPutState) {
                when (val rt = jmt.returnType) {
                    is EnumType -> if (rt.enum.labels != state.labels) diagnostics.add(Diagnostic(""))
                    is PrimitiveTypes.Boolean -> if(rt.labels != state.labels) diagnostics.add(Diagnostic(""))
                    else -> diagnostics.add(Diagnostic(""))
                }
            }
        } else {
            diagnostics.add(Diagnostic(""))
        }
    }
    return diagnostics
}

fun chkOvr(clazz: JavaClass, superclass: JavaClass): List<Diagnostic> {
    val diagnostics = mutableListOf<Diagnostic>()
    for (overrider in clazz.meths) {
        val overridden = superclass.allMeths.find { overrider overrides it }
        if (overridden != null) {
            if (!overrider.returnType.sub(overridden.returnType))
                diagnostics.add(Diagnostic(""))
           for (i in 0 until overrider.parametersType.size) {
               if (!overridden.parametersType[i].sub(overrider.parametersType[i]))
                   diagnostics.add(Diagnostic(""))
           }
        }
    }
    return diagnostics
}