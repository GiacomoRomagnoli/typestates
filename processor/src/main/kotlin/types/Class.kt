package types

import processor.environment.Java
import processor.environment.linearClassOf
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
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