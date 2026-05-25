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
}