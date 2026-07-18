package rules.dsl

class Rule<I, O>(
    val name: String,
    private val body: (I) -> RuleResult<O>
) {
    operator fun invoke(input: I): RuleResult<O> =
        body(input)
}

sealed interface RuleResult<out O> {
    data object NotApplicable : RuleResult<Nothing>
    data class Success<O>(val value: O) : RuleResult<O>
    data class Failure(val error: String) : RuleResult<Nothing>
}