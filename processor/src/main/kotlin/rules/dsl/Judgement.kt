package rules.dsl

class Judgement<I : Traceable, O>(val rules: Set<Rule<I, O>>) {
    operator fun get(name: String) = rules.firstOrNull { it.name == name }
    operator fun invoke(input: I): JudgementResult<I, O> {
        val applicable = rules
            .map { rule -> rule.name to rule(input) }
            .filter { (_, result) -> result !is RuleResult.NotApplicable }

        return when (applicable.size) {
            0 -> JudgementResult.NoApplicableRule(input)

            1 -> when (val result = applicable[0].second) {
                is RuleResult.Success ->
                    JudgementResult.Derived(applicable[0].first, result.value)

                is RuleResult.Failure ->
                    JudgementResult.NotDerived(applicable[0].first, input, result.cause)

                is RuleResult.NotApplicable ->
                    error("unreachable")
            }

            else -> JudgementResult.MultipleApplicableRules(applicable.map { it.first }.toSet(), input)
        }
    }
}

sealed interface JudgementResult<out I: Traceable, out O> {
    data class Derived<O>(
        val rule: String,
        val value: O
    ) : JudgementResult<Nothing, O>

    data class NotDerived<I : Traceable>(
        val rule: String,
        val input: I,
        val cause: JudgementResult<*, *>? = null
    ) : JudgementResult<I, Nothing>

    data class NoApplicableRule<I : Traceable>(
        val input: I
    ) : JudgementResult<I, Nothing>

    data class MultipleApplicableRules<I : Traceable>(
        val rules: Set<String>,
        val input: I
    ) : JudgementResult<I, Nothing>
}