package language.model

import com.sun.source.util.Trees
import protocol.ProtocolBinding
import protocol.model.Protocol
import javax.lang.model.element.ElementKind
import javax.lang.model.util.Elements

class Program(private val elements: Elements, private val trees: Trees) {
    private val classes = mutableMapOf<String, JavaClass>()
    operator fun get(qualifiedName: String) = classes[qualifiedName]
    fun add(javaClass: JavaClass) { classes[javaClass.qualifiedName] = javaClass }
    val allClasses = classes.values

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