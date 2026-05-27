package semantic.model

sealed interface State {
    fun simulates(w2: State, r: Set<Pair<TypeState, TypeState>> = setOf()): Boolean
}