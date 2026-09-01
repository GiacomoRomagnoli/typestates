package rules.dsl

typealias Premise<I, P> = I.() -> P
class PremiseFailure(val chain: JudgementResult<*, *>? = null) : RuntimeException(null, null, false, false)

class RuleScope<I, P, O> {
    private var premise: Premise<I, P>? = null
    private var conclusion: Conclusion<I, P, O>? = null

    fun fail(): Nothing = throw PremiseFailure()

    fun ensure(condition: Boolean) { if (!condition) fail() }

    fun <A : Traceable, B> Judgement<A, B>.derive(input: A): B =
        when (val result = this(input)) {
            is JudgementResult.Derived -> result.value
            else -> throw PremiseFailure(result)
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
                RuleResult.NotApplicable
            }
            else {
                val p = try { premise(input) }
                catch (failure: PremiseFailure) { return@Rule RuleResult.Failure(failure.chain) }
                RuleResult.Success(conclusion.right(input, p))
            }
        }
    }
}
