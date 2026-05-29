package semantic.model

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

    private fun simulates(u2: TypeState, r: Set<Pair<TypeState, TypeState>> = setOf()): Boolean {
        if (u2.isDroppable) if(!this.isDroppable) return false
        return u2.methods()
            .map { this[it] to u2[it] }
            .all { (w1, w2) -> w1 != null && w2 != null && (r.contains(w1 to w2) || w1.simulates(w2, r))}
    }

    private fun simulates(w2: OutPutState, r: Set<Pair<TypeState, TypeState>> = setOf()) =
        w2.labels()
            .map { this to w2[it] }
            .all { (u1, u2) -> u2 != null && (r.contains(u1 to u2) || u1.simulates(u2, r)) }

    override fun simulates(w2: State, r: Set<Pair<TypeState, TypeState>>) =
         when(w2) {
            is TypeState -> this.simulates(w2, r + (this to w2))
            is OutPutState -> this.simulates(w2, r)
        }
}