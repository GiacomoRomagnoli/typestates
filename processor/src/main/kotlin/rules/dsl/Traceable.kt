package rules.dsl

interface Traceable {
    fun trace(): String
}

fun JudgementResult<*, *>.formatTrace(depth: Int = 0): String {
    val prefix = if (depth == 0) "" else "    ".repeat(depth - 1) + "└── "
    return when (this) {
        is JudgementResult.NotDerived -> buildString {
            append("$prefix$rule on ${input.trace()}")
            cause?.let {
                append("\n${it.formatTrace(depth + 1)}")
            }
        }

        is JudgementResult.NoApplicableRule ->
            "${prefix}no applicable rule on ${input.trace()}"

        is JudgementResult.MultipleApplicableRules ->
            "${prefix}multiple applicable rules on ${input.trace()}: ${rules.joinToString()}"

        is JudgementResult.Derived ->
            "${prefix}derived by $rule"
    }
}