package types

import processor.environment.Java
import processor.environment.allMeths
import processor.environment.linearClassOf
import javax.lang.model.element.TypeElement

sealed interface Class {
    val element: TypeElement
}

fun allMeths(c: Class) = Java.env.allMeths(c.element)
// TODO Object è suprclasse di tutte le classi e viene considerata come tale
fun sup(c: Class) =
    (Java.types.asElement(c.element.superclass) as? TypeElement)?.let {
        if (it.getAnnotation(Typestate::class.java) != null) {
            Java.env.linearClassOf(it)
        } else NonLinearClass(it)
    } ?: c
