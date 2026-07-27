package rules.dsl

typealias Premise<I, P> = I.() -> P
class PremiseFailure : RuntimeException(null, null, false, false)

class RuleScope<I, P, O> {
    private var premise: Premise<I, P>? = null
    private var conclusion: Conclusion<I, P, O>? = null

    fun fail(): Nothing = throw PremiseFailure()

    fun ensure(condition: Boolean) { if (!condition) fail() }

    fun <A, B> Judgement<A, B>.derive(input: A): B =
        when (val result = this(input)) {
            is JudgementResult.Derived -> result.value
            else -> fail()
        }

    fun premise(block: Premise<I, P>) {
        check(premise == null)
        premise = block
    }

    fun conclusion(block: ConclusionScope<I, P, O>.() -> Unit) {
        check(conclusion == null)
        conclusion = ConclusionScope<I, P, O>().apply(block).build()
    }

    internal fun build(name: String): Rule<I, O> {
        val premise = checkNotNull(premise)
        val conclusion = checkNotNull(conclusion)
        return Rule(name) { input ->
            if (!conclusion.left(input)) {
                RuleResult.Failure
            } else try {
                val p = premise(input)
                RuleResult.Success(conclusion.right(input, p))
            } catch (_: PremiseFailure) {
                RuleResult.Failure
            }
        }
    }
}
