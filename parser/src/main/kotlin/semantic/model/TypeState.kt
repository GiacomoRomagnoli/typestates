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
}