package protocol.model

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

    infix fun sub(other: Protocol) = initState simulates other.initState

    val transitions by lazy {
        protIn.flatMap { it.methods.map { m -> Transition(m, it[m]!!) } }
    }

    val protIn by lazy {
        reach(setOf(initState), emptySet())
    }

    val protSt by lazy {
        protIn.flatMap { it.outPutStates + it }.toSet()
    }
}

