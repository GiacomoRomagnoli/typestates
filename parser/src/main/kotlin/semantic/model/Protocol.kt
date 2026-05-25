package semantic.model

class Protocol internal constructor(
    val name: String
) {
    lateinit var initState: TypeState
        internal set

    internal val typeStates = mutableMapOf<String, TypeState>()

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
}

