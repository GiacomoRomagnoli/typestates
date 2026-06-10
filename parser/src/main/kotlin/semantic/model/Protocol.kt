package semantic.model

class Protocol internal constructor(
    val name: String
) {
    lateinit var initState: TypeState
        internal set

    internal val typeStates = mutableMapOf<String, TypeState>()

    operator fun get(key: String) = typeStates[key]

    private fun reach(frontier: Set<TypeState>, visited: Set<TypeState>): Set<TypeState> {
        val discovered = frontier.flatMap { it.typeStates }.toSet()
        val newStates = discovered - visited
        return if (newStates.isEmpty()) {
            visited + frontier
        } else {
            reach(newStates, visited + frontier)
        }
    }

    val protIn
        get() = reach(setOf(initState), emptySet())

    val protSt
        get() = protIn.flatMap { it.outPutStates + it }.toSet()
}

