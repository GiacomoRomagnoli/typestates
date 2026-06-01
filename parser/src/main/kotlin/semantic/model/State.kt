package semantic.model

sealed class State {
    internal abstract fun simulates(w2: State, r: Set<Pair<TypeState, TypeState>>): Boolean
    infix fun simulates(w2: State) = simulates(w2, emptySet())
}