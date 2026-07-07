package protocol

import ast.JavaTypeNode
import ast.MethodNode
import ast.OutPutStateNode
import ast.TypeStateNode
import protocol.model.JavaType
import protocol.model.Method
import protocol.model.OutPutState
import protocol.model.TypeState
import java.util.IdentityHashMap

class ProtocolBinding {
    private val javaTypes = IdentityHashMap<JavaType, JavaTypeNode>()
    private val methods = IdentityHashMap<Method, MethodNode>()
    private val outputStates = IdentityHashMap<OutPutState, OutPutStateNode>()
    private val typeStates = IdentityHashMap<TypeState, TypeStateNode>()

    fun associate(type: JavaType, node: JavaTypeNode) {
        javaTypes[type] = node
    }

    fun associate(method: Method, node: MethodNode) {
        methods[method] = node
    }

    fun associate(state: TypeState, node: TypeStateNode) {
        typeStates[state] = node
    }

    fun associate(output: OutPutState, node: OutPutStateNode) {
        outputStates[output] = node
    }

    fun nodeOf(type: JavaType): JavaTypeNode? =
        javaTypes[type]

    fun nodeOf(method: Method): MethodNode? =
        methods[method]

    fun nodeOf(state: TypeState): TypeStateNode? =
        typeStates[state]

    fun nodeOf(output: OutPutState): OutPutStateNode? =
        outputStates[output]
}