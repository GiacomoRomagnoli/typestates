package types

import processor.environment.Java


data class QualifiedType(val c: Class, val t: Type)

val NonLinearClass.typed get() = QualifiedType(this, Shared)
infix fun LinearClass.typed(t: Type) = QualifiedType(this, t)
infix fun QualifiedType.sub(other: QualifiedType) =
    Java.types.isSubtype(c.element.asType(), other.c.element.asType()) && t sub other.t &&
    when {
        this.c is LinearClass && other.c is LinearClass -> protIn(c).containsAll(typestates(t))
                && protIn(other.c).containsAll(typestates(other.t))
        else -> true
    }
