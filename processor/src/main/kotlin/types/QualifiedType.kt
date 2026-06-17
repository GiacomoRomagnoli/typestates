package types

import classes.Class
import classes.LinearClass
import classes.NonLinearClass
import classes.protIn
import processor.environment.Java


data class QualifiedType(val c: Class, val t: Type)

infix fun Class.typed(t: Type) = QualifiedType(this, t)
infix fun QualifiedType.sub(other: QualifiedType): Boolean {
    if (!Java.types.isSubtype(c.element.asType(), other.c.element.asType())) return false
    if (c is LinearClass && !typestates(t).all { it in protIn(c) }) return false
    else if (c is NonLinearClass && typestates(t).isNotEmpty()) return false
    if (other.c is LinearClass && !typestates(other.t).all { it in protIn(other.c) }) return false
    else if (other.c is NonLinearClass && typestates(other.t).isNotEmpty()) return false
    return t sub other.t
}
