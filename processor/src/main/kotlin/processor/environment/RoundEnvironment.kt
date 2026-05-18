package processor.environment

import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

fun RoundEnvironment.linearTypes() =
    getElementsAnnotatedWith(Typestate::class.java).map { it as TypeElement }