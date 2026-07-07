package language.model

import protocol.ProtocolBinding
import protocol.model.Protocol

class Program {
    private val classes = mutableMapOf<String, JavaClass>()
    operator fun get(qualifiedName: String) = classes[qualifiedName]
    fun add(javaClass: JavaClass) { classes[javaClass.qualifiedName] = javaClass }
    val allClasses = classes.values

    private val protocols = mutableMapOf<Protocol, ProtocolBinding>()
    operator fun get(protocol: Protocol) = protocols[protocol]
    fun add(protocol: Protocol, binding: ProtocolBinding) { protocols[protocol] = binding }

}