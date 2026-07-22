package rules.dsl

import kotlin.experimental.ExperimentalTypeInference

class JudgementScope<I, O> {
    private val rules = mutableListOf<Rule<I, O>>()

    @OptIn(ExperimentalTypeInference::class)
    fun <P : Any> rule(name: String, @BuilderInference block: RuleScope<I, P, O>.() -> Unit) {
        rules += RuleScope<I, P, O>().apply(block).build(name)
    }

    internal fun build(): Judgement<I, O> = Judgement(rules.toSet())
}

fun <I, O> judgement(block: JudgementScope<I, O>.() -> Unit): Judgement<I, O> =
    JudgementScope<I, O>().apply(block).build()