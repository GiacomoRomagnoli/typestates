package language.types

import language.model.JavaClass
import javax.lang.model.util.Types


data class ClassType(val clazz: JavaClass, val type: T, private val typeUtils: Types) : Type {
    override fun sub(other: Type) = if (other is ClassType) this.sub(other) else false
    val isWellFormed by lazy {
        if (clazz.isLinear)
            typestates(type).all { it in clazz.protocol!!.protIn }
        else
            typestates(type).isEmpty()
    }
    private fun sub(other: ClassType) =
        typeUtils.isSubtype(clazz.element.asType(), other.clazz.element.asType()) &&
                this.isWellFormed && other.isWellFormed && this.type sub other.type
}
