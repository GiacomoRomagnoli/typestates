package semantic.model

import ast.OutPutStateNode

class OutPutState private constructor(
    private val protocol: Protocol,
    private val branches: Map<String, String>
) : State {

    operator fun get(label: String) =
        when (val ref = branches[label]) {
            null -> null
            else -> protocol[ref]
        }

    fun labels() = branches.keys

    fun typeStates() = branches.values.mapNotNull { protocol[it] }.toSet()

    companion object {
        fun build(node: OutPutStateNode, protocol: Protocol): OutPutState =
            OutPutState(protocol, node.branches.associate { it.label.value to it.ref.name.value })
    }
}