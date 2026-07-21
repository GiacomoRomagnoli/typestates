package rules.dsl

class Rule<in I, out O>(
    val name: String,
    private val body: (I) -> RuleResult<O>
) {
    operator fun invoke(input: I): RuleResult<O> =
        body(input)
}

sealed interface RuleResult<out O> {
    data class Success<O>(val value: O) : RuleResult<O>
    data object Failure : RuleResult<Nothing>
}