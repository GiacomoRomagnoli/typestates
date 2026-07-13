package rules

import language.model.JavaClass
import language.model.JavaMethod
import language.types.EnumType
import language.types.PrimitiveTypes
import protocol.model.Method
import protocol.model.OutPutState

sealed interface Diagnostic
data class MissingMethod(val method: Method) : Diagnostic
data class NonExhaustiveOutPutState(val outputState: OutPutState, val labels: List<String>) : Diagnostic
data class UnexpectedOutPutState(val outputState: OutPutState, val method: JavaMethod) : Diagnostic
data class InvalidOverride(val overrider: JavaMethod, val overridden: JavaMethod) : Diagnostic

fun chkProt(clazz: JavaClass): List<Diagnostic> {
    require(clazz.isLinear)
    val diagnostics = mutableListOf<Diagnostic>()
    for (transition in clazz.protocol!!.transitions) {
        val jmt = clazz.allMeths.find { it.sign == transition.method }
        if (jmt != null) {
            val state = transition.state
            if (state is OutPutState) {
                when (val rt = jmt.returnType) {
                    is EnumType ->
                        if (rt.enum.labels != state.labels)
                            diagnostics.add(NonExhaustiveOutPutState(state, rt.enum.labels))
                    is PrimitiveTypes.Boolean ->
                        if(rt.labels != state.labels)
                            diagnostics.add(NonExhaustiveOutPutState(state, rt.labels))
                    else -> diagnostics.add(UnexpectedOutPutState(state, jmt))
                }
            }
        } else {
            diagnostics.add(MissingMethod(transition.method))
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
                diagnostics.add(InvalidOverride(overrider, overridden))
           for (i in 0 until overrider.parametersType.size) {
               if (!overridden.parametersType[i].sub(overrider.parametersType[i]))
                   diagnostics.add(InvalidOverride(overrider, overridden))
           }
        }
    }
    return diagnostics
}