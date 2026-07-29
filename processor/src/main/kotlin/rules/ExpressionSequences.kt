package rules

import com.sun.source.util.TreePath
import language.model.Program
import language.types.TC
import language.types.TypeEnv
import language.types.resolve
import rules.dsl.Judgement
import rules.dsl.judgement

object ExprSeq {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val expressions: List<TreePath>,
        val assign: Boolean,
        val program: Program,
        val tcs: List<TC> = emptyList()
    )
    data class Right(
        val tcs: List<TC>,
        val fields: TypeEnv,
        val variables: TypeEnv
    )
}

val EXPRESSION_SEQUENCE_JUDGEMENT: Judgement<ExprSeq.Left, ExprSeq.Right> =
    judgement {

        rule("TEmptyExp") {
            premise {  }
            conclusion {
                left { expressions.isEmpty() }
                right { ExprSeq.Right(tcs, fields, variables) }
            }
        }

        rule("TSeqExp") {
            premise {
                val exprL = Expr.Left(fields, variables, expressions.first(), assign, program)
                val exprR = EXPRESSION_JUDGEMENT.derive(exprL)
                val exprSeqL = copy(
                    fields = resolve(exprR.fields),
                    variables = resolve(exprR.variables),
                    expressions = expressions.drop(1),
                    tcs = tcs + exprR.tc
                )
                EXPRESSION_SEQUENCE_JUDGEMENT.derive(exprSeqL)
            }
            conclusion {
                left { expressions.isNotEmpty() }
                right { it }
            }
        }
    }