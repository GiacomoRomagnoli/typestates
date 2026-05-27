package semantic.model

class OutPutState internal constructor(
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

    fun simulates(w2: OutPutState, r: Set<Pair<TypeState, TypeState>> = setOf()) =
        this.labels()
            .map { this[it] to w2[it] }
            .all { (u1, u2) -> u2 != null && u1 != null && r.contains(u1 to u2) || u1!!.simulates(u2!!, r + (u1 to u2)) }

    fun simulates(u2: TypeState, r: Set<Pair<TypeState, TypeState>> = setOf()) =
        this.labels()
            .map { this[it] to u2}
            .all { (u1, u2) -> u1 != null && r.contains(u1 to u2) || u1!!.simulates(u2, r + (u1 to u2)) }

    override fun simulates(w2: State, r: Set<Pair<TypeState, TypeState>>) =
        when (w2) {
            is OutPutState -> this.simulates(w2, r)
            is TypeState -> this.simulates(w2, r)
        }
}