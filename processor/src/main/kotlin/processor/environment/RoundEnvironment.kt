package processor.environment

import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

fun RoundEnvironment.linearTypes() =
    getElementsAnnotatedWith(Typestate::class.java).map { it as TypeElement }
fun RoundEnvironment.annotatedTypeElements() = (
        getElementsAnnotatedWith(Typestate::class.java) +
            getElementsAnnotatedWith(Ensures::class.java).map { it.enclosingElement } +
            getElementsAnnotatedWith(Requires::class.java).map { it.enclosingElement.enclosingElement }
        ).map { it as TypeElement }.toSet()