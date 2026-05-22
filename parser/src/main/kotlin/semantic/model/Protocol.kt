package semantic.model

import ast.ProtocolNode


class Protocol private constructor(
    val name: String
) {
    lateinit var initState: TypeState
        internal set

    private val typeStates = mutableMapOf<String, TypeState>()

    private fun addTypeState(typeState: TypeState) = typeStates.putIfAbsent(typeState.name, typeState)

    operator fun get(key: String) = typeStates[key]

    fun protIn(): Set<TypeState> {
        fun reach(frontier: Set<TypeState>, visited: Set<TypeState>): Set<TypeState> {
            val discovered = frontier.flatMap { it.typeStates() }.toSet()
            val newStates = discovered - visited
            return if (newStates.isEmpty()) {
                visited + frontier
            } else {
                reach(newStates, visited + frontier)
            }
        }
        return reach(setOf(initState), emptySet())
    }

    companion object {
        fun build(ast: ProtocolNode): Protocol {
            val protocol = Protocol(ast.name.value)
            val typeStates = ast.states.map { TypeState.build(it, protocol) }
            protocol.initState = typeStates.first()
            typeStates.forEach { protocol.addTypeState(it) }
            return protocol
        }
    }
}

