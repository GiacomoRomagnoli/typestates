package rules

import com.sun.source.util.TreePath
import language.model.Program
import language.types.TC
import language.types.TypeEnv
import language.types.resolve
import rules.dsl.Judgement
import rules.dsl.Traceable
import rules.dsl.judgement

object ExprSeq {
    data class Left(
        val Tf: TypeEnv,
        val Ts: TypeEnv,
        val a: Boolean,
        val program: Program,
        val exprs: List<TreePath>,
        val tcs: List<TC> = emptyList()
    ): Traceable {
        override fun trace(): String =
            if (exprs.isEmpty()) "<empty>" else "${exprs.size} expression(s), next: ${exprs.first().leaf}"
    }
    data class Right(
        val tcs: List<TC>,
        val Tf: TypeEnv,
        val Ts: TypeEnv
    )
}

val EXPRESSION_SEQUENCE_JUDGMENT: Judgement<ExprSeq.Left, ExprSeq.Right> =
    judgement {

        rule("TEmptyExp") {
            premise {  }
            conclusion {
                left { exprs.isEmpty() }
                right { ExprSeq.Right(tcs, Tf, Ts) }
            }
        }

        rule("TSeqExp") {
            premise {
                val exprL = Expr.Left(Tf, Ts, exprs.first(), a, program)
                val exprR = EXPRESSION_JUDGMENT.derive(exprL)
                val exprSeqL = copy(
                    Tf = resolve(exprR.Tf),
                    Ts = resolve(exprR.Ts),
                    exprs = exprs.drop(1),
                    tcs = tcs + exprR.tc
                )
                EXPRESSION_SEQUENCE_JUDGMENT.derive(exprSeqL)
            }
            conclusion {
                left { exprs.isNotEmpty() }
                right { it }
            }
        }
    }