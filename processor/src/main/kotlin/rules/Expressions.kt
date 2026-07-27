package rules

import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.NewClassTree
import com.sun.source.util.TreePath
import language.model.JavaClass
import language.model.Program
import language.types.Delta
import language.types.Shared
import language.types.TC
import language.types.TypeEnv
import language.types.U
import language.types.fields
import language.types.resolve
import language.types.sub
import language.types.term
import language.types.toTC
import language.types.tt
import rules.dsl.Judgement
import rules.dsl.judgement

data class LocatedExpression(val expr: ExpressionTree, val path: TreePath) { init { require(path.leaf == expr) } }

object Expr {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        private val locatedExpression: LocatedExpression,
        val assign: Boolean,
        val program: Program
    ) {
        val expression get() = locatedExpression.expr
        val path get() = locatedExpression.path
    }
    data class Right(
        val type: TC,
        val fields: TypeEnv,
        val variables: TypeEnv
    )
}

val typingExpression = judgement<Expr.Left, Expr.Right> {
    rule("TNew") {
        premise {
            val newExpr = expression as NewClassTree
            val c = program.asJavaClass(TreePath(path, newExpr.identifier)) ?: fail()
            ensure(assign || c.protocol?.let { term(U(it.initState)) } ?: true)
            val args = newExpr.arguments.map { LocatedExpression(it, TreePath(path, it)) }
            val exprSeqL = ExprSeq.Left(fields, variables, args, true, program)
            val exprSeqR = typingExpressionSequence.derive(exprSeqL)
            val constructor = program.asJavaConstructor(path) ?: fail()
            ensure(exprSeqR.types.zip(constructor.pt).all { (tc, pt) -> tc sub toTC(pt) })
            c to (exprSeqR.fields to exprSeqR.variables)
        }
        conclusion {
            left { expression is NewClassTree }
            right { premise: Pair<JavaClass, Delta> ->
                val (c, delta) = premise
                Expr.Right(
                    tt(c, c.protocol?.let { U(it.initState) } ?: Shared),
                    delta.fields,
                    delta.second
                )
            }
        }
    }
}


object ExprSeq {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val expressions: List<LocatedExpression>,
        val assign: Boolean,
        val program: Program,
        val types: List<TC> = emptyList()
    )
    data class Right(
        val types: List<TC>,
        val fields: TypeEnv,
        val variables: TypeEnv
    )
}

val typingExpressionSequence: Judgement<ExprSeq.Left, ExprSeq.Right> =
    judgement {

        rule("TEmptyExp") {
            premise {  }
            conclusion {
                left { expressions.isEmpty() }
                right { ExprSeq.Right(types, fields, variables) }
            }
        }

        rule("TSeqExp") {
            premise {
                val exprL = Expr.Left(fields, variables, expressions.first(), assign, program)
                val exprR = typingExpression.derive(exprL)
                val exprSeqL = copy(
                    fields = resolve(exprR.fields),
                    variables = resolve(exprR.variables),
                    expressions = expressions.drop(1),
                    types = types + exprR.type
                )
                typingExpressionSequence.derive(exprSeqL)
            }
            conclusion {
                left { expressions.isNotEmpty() }
                right { it }
            }
        }
    }