package rules

import com.sun.source.tree.ExpressionTree
import language.model.Program
import language.types.TC
import rules.dsl.Judgement

object Expression {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val expression: ExpressionTree,
        val assign: Boolean,
        val program: Program
    )

    data class Right(
        val type: TC,
        val fields: TypeEnv,
        val variables: TypeEnv
    )
}

val typingExpression = Judgement<Expression.Left, Expression.Right>()