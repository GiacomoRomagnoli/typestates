package rules.dsl

class Judgement<I, O>(val rules: Set<Rule<I, O>>) {
    operator fun get(name: String) = rules.firstOrNull { it.name == name }
    operator fun invoke(input: I): JudgementResult<O> = rules
        .mapNotNull { rule ->
            when(val result = rule(input)) {
                is RuleResult.Failure -> null
                is RuleResult.Success -> rule.name to result.value
            }
        }
        .let {
            when(it.size) {
                0 -> JudgementResult.NotDerivable
                1 -> JudgementResult.Derived(it[0].first, it[0].second)
                else -> JudgementResult.Ambiguous(it.map { (name, _) -> name }.toSet())
            }
        }
}

sealed interface JudgementResult<out O> {
    data class Derived<O>(val rule: String, val value: O) : JudgementResult<O>
    data class Ambiguous(val rules: Set<String>) : JudgementResult<Nothing>
    data object NotDerivable : JudgementResult<Nothing>
}