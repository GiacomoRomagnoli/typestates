package semantic.model

import ast.OutPutStateNode
import ast.TypeStateNode
import ast.TypeStateRefNode

class TypeState internal constructor(
    val name: String,
    val isDroppable: Boolean,
    private val protocol: Protocol,
    private val typeStateTransitions: Map<Method, String>,
    private val outPutStateTransitions: Map<Method, OutPutState>
) : State {

    operator fun get(method: Method) =
        when (val ref = typeStateTransitions[method]) {
            null -> outPutStateTransitions[method]
            else -> protocol[ref]
        }

    fun methods() = typeStateTransitions.keys + outPutStateTransitions.keys

    fun typeStates(): Set<TypeState> =
        typeStateTransitions.values
            .mapNotNull { protocol[it] }
            .plus(outPutStateTransitions.values.flatMap { it.typeStates() })
            .toSet()

    internal companion object {
        fun build(node: TypeStateNode, protocol: Protocol): TypeState =
            TypeState(
                node.name.value,
                node.droppable,
                protocol,
                node.transitions
                    .filter { it.target is TypeStateRefNode }
                    .associate { Method.build(it.method) to (it.target as TypeStateRefNode).name.value },
                node.transitions
                    .filter { it.target is OutPutStateNode }
                    .associate { Method.build(it.method) to OutPutState.build(it.target as OutPutStateNode, protocol) }
            )
    }
}