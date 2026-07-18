package rules.dsl

import kotlin.experimental.ExperimentalTypeInference

class Judgement<I, O>(val name: String) {
    private val rules = mutableListOf<Rule<I, O>>()

    internal fun register(rule: Rule<I, O>) { rules += rule }

    @OptIn(ExperimentalTypeInference::class)
    fun <P : Any> rule(name: String, @BuilderInference block: RuleScope<I, P, O>.() -> Unit): Rule<I, O> =
        RuleScope<I, P, O>().apply(block).build(name).also(::register)

    operator fun invoke(input: I): JudgementResult<O> {
        for (rule in rules) {
            return when (val result = rule(input)) {
                RuleResult.NotApplicable -> continue
                is RuleResult.Failure -> JudgementResult.Rejected("${rule.name} : ${result.error}")
                is RuleResult.Success -> JudgementResult.Derived(result.value)
            }
        }
        return JudgementResult.NotDerivable
    }
}

sealed interface JudgementResult<out O> {
    data class Derived<O>(val value: O) : JudgementResult<O>
    data class Rejected(val error: String) : JudgementResult<Nothing>
    data object NotDerivable : JudgementResult<Nothing>
}