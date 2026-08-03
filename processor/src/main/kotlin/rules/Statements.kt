package rules

import com.sun.source.tree.ExpressionStatementTree
import com.sun.source.util.TreePath
import language.model.Program
import language.types.Delta
import language.types.TypeEnv
import language.types.fields
import language.types.resolve
import language.types.variables
import rules.dsl.Judgement
import rules.dsl.judgement

object Stmt {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val path: TreePath,
        val program: Program,
    ) {
        val statement get() = path.leaf
    }

    data class Right(
        val fields: TypeEnv,
        val variables: TypeEnv,
    )
}

val STATEMENT_JUDGMENT: Judgement<Stmt.Left, Stmt.Right> = judgement {

    rule<Delta>("TExp") {
        premise {
            val stmt = statement as ExpressionStatementTree
            val expr = TreePath(path, stmt.expression)
            val result = EXPRESSION_JUDGMENT.derive(
                Expr.Left(fields, variables, expr, false, program)
            )
            result.fields to result.variables
        }
        conclusion {
            left { statement is ExpressionStatementTree }
            right { Stmt.Right(resolve(it.fields), resolve(it.variables)) }
        }
    }
}