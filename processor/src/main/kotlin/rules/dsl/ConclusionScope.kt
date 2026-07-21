package rules.dsl

data class Conclusion<I, P, O>(val left: I.() -> Boolean, val right: I.(P) -> O)

class ConclusionScope<I, P, O> {
    private var left: (I.() -> Boolean)? = null
    private var right: (I.(P) -> O)? = null

    fun left(block: I.() -> Boolean) {
        check(left == null)
        left = block
    }

    fun right(block: I.(P) -> O) {
        check(right == null)
        right = block
    }

    internal fun build(): Conclusion<I, P, O> {
        check(left != null && right != null)
        return Conclusion(left!!, right!!)
    }
}

