package rules.utils

import language.model.JavaClass
import language.types.Bool
import language.types.ClassType
import language.types.EnumType
import language.types.sub
import protocol.model.OutPutState

fun chkProt(clazz: JavaClass): Boolean {
    require(clazz.isLinear)
    for (transition in clazz.protocol!!.transitions) {
        val jmt = clazz.allMeths.find { it.sign == transition.method }
        if (jmt != null) {
            val state = transition.state
            if (state is OutPutState) {
                when (val rt = jmt.rt) {
                    is EnumType -> if (!rt.enum.labels.containsAll(state.labels)) return false
                    is Bool -> if (!rt.labels.containsAll(state.labels)) return false
                    else -> return false
                }
            }
        } else return false
    }
    return true
}

fun chkOvr(clazz: JavaClass, superclass: JavaClass): Boolean {
    for (overrider in clazz.meths) {
        val overridden = superclass.allMeths.find { overrider overrides it }
        if (overridden != null) {
            val overriderRt = overrider.rt
            val overriddenRt = overridden.rt
            if (overriderRt is ClassType && overriddenRt is ClassType && !(overriderRt sub overriddenRt))
                return false
           for (i in 0 until overrider.pt.size) {
               val overriderPT = overrider.pt[i]
               val overriddenPT = overridden.pt[i]
               if (overriderPT is ClassType && overriddenPT is ClassType && !(overriddenPT sub overriderPT))
                   return false
           }
        }
    }
    return true
}