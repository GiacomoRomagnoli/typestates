package rules.dsl

class RuleScope<I, P : Any, O> {
    private val matches = mutableListOf<I.() -> Boolean>()
    private var premise: Premise<I, P>? = null
    private var conclusion: (I.(P) -> O)? = null

    fun match(block: I.() -> Boolean) { matches += block }

    fun premise(block: PremiseScope<I, P>.() -> Unit) {
        check(premise == null)
        premise = PremiseScope<I, P>().apply(block).build()
    }

    fun conclusion(block: I.(P) -> O) {
        check(conclusion == null)
        conclusion = block
    }

    internal fun build(name: String): Rule<I, O> =
        Rule(name) { input -> evaluate(input) }

    private fun evaluate(input: I): RuleResult<O> {
        if (matches.any { !input.it() }) return RuleResult.NotApplicable
        val premise = premise ?: return RuleResult.Failure("missing premise")
        val conclusion = conclusion ?: return RuleResult.Failure("missing conclusion")

        return when (val result = premise(input)) {
            is RuleResult.Success ->
                RuleResult.Success(input.conclusion(result.value))
            is RuleResult.Failure -> result
            RuleResult.NotApplicable -> RuleResult.NotApplicable
        }
    }
}