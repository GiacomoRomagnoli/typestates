package rules.dsl

typealias Premise<I, P> = (I) -> RuleResult<P>

class PremiseScope<I, P> {
    private val conditions = mutableListOf<I.() -> Boolean>()
    private var derivation: (I.() -> RuleResult<P>)? = null

    fun condition(block: I.() -> Boolean) {
        conditions += block
    }

    fun derivation(block: I.() -> RuleResult<P>) {
        check(derivation == null)
        derivation = block
    }

    internal fun build(): Premise<I, P> {
        val derive = checkNotNull(derivation)
        return { input ->
            if (conditions.any { !it(input) })
                RuleResult.Failure("premise failed")
            else input.derive()
        }
    }
}

fun <I> PremiseScope<I, Unit>.satisfied() {
    derivation { RuleResult.Success(Unit) }
}