package language.model

import com.sun.source.util.TreePath
import com.sun.source.util.Trees
import protocol.ProtocolBinding
import protocol.model.Protocol
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

class Program(
    private val elements: Elements,
    private val trees: Trees
) {
    private val classes = mutableMapOf<String, JavaClass>()
    val allClasses = classes.values

    operator fun get(qualifiedName: String) = classes[qualifiedName]

    fun add(javaClass: JavaClass) { classes[javaClass.qualifiedName] = javaClass }

    fun asJavaClass(path: TreePath) =
        (trees.getElement(path) as? TypeElement)?.let { this[it.qualifiedName.toString()] }

    fun asJavaConstructor(path: TreePath): JavaConstructor? {
        val element = trees.getElement(path) as? ExecutableElement
        return element
            ?.takeIf { it.kind == ElementKind.CONSTRUCTOR }
            ?.let { it.enclosingElement as? TypeElement }
            ?.let { this[it.qualifiedName.toString()] }
            ?.constructors
            ?.singleOrNull { it.element == element }
    }

    private val protocols = mutableMapOf<Protocol, ProtocolBinding>()
    operator fun get(protocol: Protocol) = protocols[protocol]
    fun add(protocol: Protocol, binding: ProtocolBinding) { protocols[protocol] = binding }

    private val enums = mutableMapOf<String, JavaEnum>()
    fun enumOf(qualifiedName: String): JavaEnum? =
        enums[qualifiedName]
            ?: elements.getTypeElement(qualifiedName)
                ?.takeIf { it.kind == ElementKind.ENUM }
                ?.let(::JavaEnum)
                ?.also { enums[it.qualifiedName] = it }

    infix fun containsEnum(name: String) = enumOf(name) != null

}