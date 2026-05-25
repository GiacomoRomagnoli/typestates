import ast.JavaTypeNode
import ast.MethodNode
import ast.OutPutStateNode
import ast.ProtocolNode
import ast.TypeStateNode
import ast.TypeStateRefNode
import semantic.model.JavaType
import semantic.model.Method
import semantic.model.OutPutState
import semantic.model.Protocol
import semantic.model.TypeState

class ProtocolContext(val ast: ProtocolNode) {
    private val methods = mutableMapOf<Method, MethodNode>()
    private val outputStates = mutableMapOf<OutPutState, OutPutStateNode>()
    private val typeStates = mutableMapOf<TypeState, TypeStateNode>()
    private val javaTypes = mutableMapOf<JavaType, JavaTypeNode>()
    var model: Protocol

    init {
        model = build(ast)
    }

    fun nodeOf(javaType: JavaType) = javaTypes[javaType]
    private fun build(node: JavaTypeNode): JavaType {
        val javaType = JavaType(node.name.joinToString(".") { it.value }, node.arrayLevel)
        javaTypes[javaType] = node
        return javaType
    }

    fun nodeOf(method: Method) = methods[method]
    private fun build(node: MethodNode): Method {
        val method = Method(node.name.value, node.args.map { build(it) })
        methods[method] = node
        return method
    }

    fun nodeOf(outPutState: OutPutState) = outputStates[outPutState]
    private fun build(node: OutPutStateNode, protocol: Protocol): OutPutState {
        val outPutState = OutPutState(protocol, node.branches.associate { it.label.value to it.ref.name.value })
        outputStates[outPutState] = node
        return outPutState
    }

    fun nodeOf(typeState: TypeState) = typeStates[typeState]
    private fun build(node: TypeStateNode, protocol: Protocol): TypeState {
        val typeState = TypeState(
            node.name.value,
            node.droppable,
            protocol,
            node.transitions
                .filter { it.target is TypeStateRefNode }
                .associate { build(it.method) to (it.target as TypeStateRefNode).name.value },
            node.transitions
                .filter { it.target is OutPutStateNode }
                .associate { build(it.method) to build(it.target as OutPutStateNode, protocol) }
        )
        typeStates[typeState] = node
        return typeState
    }

    private fun build(ast: ProtocolNode): Protocol {
        val protocol = Protocol(ast.name.value)
        val typeStates = ast.states.map { build(it, protocol) }
        protocol.initState = typeStates.first()
        typeStates.forEach { protocol.typeStates.putIfAbsent(it.name, it) }
        return protocol
    }

}