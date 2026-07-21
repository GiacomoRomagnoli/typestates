package rules.dsl

import kotlin.experimental.ExperimentalTypeInference

class Judgement<I, O> {
    private val rules = mutableListOf<Rule<I, O>>()

    internal fun register(rule: Rule<I, O>) { rules += rule }

    @OptIn(ExperimentalTypeInference::class)
    fun <P : Any> rule(name: String, @BuilderInference block: RuleScope<I, P, O>.() -> Unit): Rule<I, O> =
        RuleScope<I, P, O>().apply(block).build(name).also(::register)

    operator fun invoke(input: I): JudgementResult<O> =
        rules.map { it.invoke(input) }
            .filterIsInstance<RuleResult.Success<O>>()
            .let { if (it.size == 1) JudgementResult.Derived(it.first().value) else JudgementResult.NotDerivable }
}

sealed interface JudgementResult<out O> {
    data class Derived<O>(val value: O) : JudgementResult<O>
    data object NotDerivable : JudgementResult<Nothing>
}