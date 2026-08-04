package language.model

import language.types.ClassType
import language.types.T
import protocol.model.Method
import protocol.model.Protocol
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

sealed interface ClassRef {
    val protocol: Protocol?
    val superclass: JavaClass?
    val isLinear: Boolean
        get() = protocol != null
}

class JavaClass(
    val element: TypeElement,
    private val declaredProtocol: Protocol?,
    private val program: Program
): ClassRef {

    infix fun isSubClassOf(other: JavaClass) =
        program.types.isSubtype(element.asType(), other.element.asType())

    override val superclass by lazy {
        (program.types.asElement(element.superclass) as? TypeElement)?.let(program::classByElement)
    }

    val constructors by lazy {
        element.enclosedElements
            .filter { it.kind == ElementKind.CONSTRUCTOR }
            .map { JavaConstructor(it as ExecutableElement, program) }
    }

    val meths by lazy {
        element.enclosedElements
            .filter { it.kind == ElementKind.METHOD }
            .map { JavaMethod(it as ExecutableElement, program) }
    }

    val allMeths: List<JavaMethod> by lazy {
        if (superclass == null) meths else meths + superclass!!.allMeths
    }

    override val protocol: Protocol? by lazy {
        declaredProtocol ?: superclass?.protocol
    }

    val fields by lazy {
        element.enclosedElements
            .filter { it.kind == ElementKind.FIELD && Modifier.STATIC !in it.modifiers}
            .map { JavaField(it as VariableElement, program) }
    }

    val qualifiedName = element.qualifiedName.toString()

    fun allF(fieldName: String): JavaClass? =
        if(fields.firstOrNull { it.name == fieldName } != null) this else superclass?.allF(fieldName)

    override fun equals(other: Any?): Boolean =
        this === other || other is JavaClass && qualifiedName == other.qualifiedName

    override fun hashCode(): Int =
        qualifiedName.hashCode()
}

/**
 * The dummy class ⊥C associated with the null literal.
 *
 * It is not a Java declaration and therefore has no TypeElement.
 */
data object BottomClass : ClassRef {
    override val protocol: Protocol? = null
    override val superclass: JavaClass? = null
    override fun toString(): String = "⊥C"
}

infix fun ClassRef.isSubClassOf(other: ClassRef) =
    when(this) {
        is BottomClass -> true
        is JavaClass -> other is JavaClass && this isSubClassOf other
    }

infix fun ClassRef.at(t: T) = ClassType(this, t)

fun anytime(c: ClassRef, mt: Method) = c.protocol?.protIn.orEmpty().none { mt in it.methods }