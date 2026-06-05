package types

import processor.environment.Java


data class QualifiedType(val c: Class, val t: Type)

infix fun QualifiedType.sub(other: QualifiedType) =
    protIn(c).containsAll(typestates(t))
            && protIn(other.c).containsAll(typestates(other.t))
            && Java.types.isSubtype(c.element.asType(), other.c.element.asType()) == true
            && t sub other.t