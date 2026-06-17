package processor.classes

import processor.elements.ensures
import processor.elements.requires
import processor.environment.Java
import processor.environment.linearClassOf
import processor.environment.match
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.collections.plus

sealed interface Class {
    val element: TypeElement
}

fun allMeths(c: Class): List<ExecutableElement> = when(val supC = sup(c)) {
    c -> meths(c)
    else -> meths(c) + allMeths(supC)
}
fun meths(c: Class) =
    c.element.enclosedElements.filter { it.kind == ElementKind.METHOD }.map { it as ExecutableElement }
fun sup(c: Class) =
    (Java.types.asElement(c.element.superclass) as? TypeElement)?.let {
        if (it.getAnnotation(Typestate::class.java) != null) {
            Java.env.linearClassOf(it)
        } else NonLinearClass(it)
    } ?: c
fun chkOvr(c: Class, supC: Class): Boolean {
    var holds = true
    val superMethods = allMeths(supC)
    for (mt in meths(c)) {
        val supMt = superMethods.find { Java.elements.overrides(mt, it, c.element) }
        if(supMt != null ) {
            if(!mt.ensures.contentEquals(supMt.ensures)) {
                Java.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    """
                        method ${mt.simpleName} declared in class ${c.element.qualifiedName} 
                        does not correctly override method ${supMt.simpleName} declared in class 
                        ${supC.element.qualifiedName} because return type is not invariant.
                    """.trimIndent(),
                    mt
                )
                holds = false
            }
            for (index in 0 until mt.parameters.size) {
                if (!mt.parameters[index].requires.contentEquals(supMt.parameters[index].requires)) {
                    Java.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        """
                        method ${mt.simpleName} declared in class ${c.element.qualifiedName} 
                        does not correctly override method ${supMt.simpleName} declared in class 
                        ${supC.element.qualifiedName} because parameter ${mt.parameters[index].simpleName} 
                        type is not invariant.
                    """.trimIndent(),
                        mt.parameters[index]
                    )
                    holds = false
                }
            }
        }
    }
    if (c is LinearClass && supC is NonLinearClass) {
        for (mt in c.protocol.protIn.flatMap { it.methods }.toSet()) {
            for (supMt in superMethods) {
                if(Java.env.match(supMt, mt)) {
                    Java.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        """
                            method ${mt.simpleName} must be anytime.
                        """.trimIndent(),
                        c.element
                    )
                    holds = false
                }
            }
        }
    }
    return holds
}