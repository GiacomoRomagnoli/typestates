package rules

import com.sun.source.tree.AssignmentTree
import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.NewClassTree
import com.sun.source.util.TreePath
import language.model.JavaClass
import language.model.Program
import language.types.Bool
import language.types.BoolUnd
import language.types.Delta
import language.types.Double
import language.types.DoubleUnd
import language.types.Eid
import language.types.EnumType
import language.types.Integer
import language.types.IntegerUnd
import language.types.Receiver
import language.types.Shared
import language.types.TC
import language.types.TypeEnv
import language.types.TypeStateTree
import language.types.get
import language.types.U
import language.types.fields
import language.types.lookup
import language.types.sub
import language.types.term
import language.types.toTC
import language.types.tt
import language.types.upd
import language.types.variables
import rules.dsl.Judgement
import rules.dsl.judgement

/**
 * data classes for partial result of rules
 */
data class TUpdB(val c: JavaClass, val eid: Eid, val tc: TC, val delta: Delta)
data class TNew(val c: JavaClass, val delta: Delta)
data class LocatedExpression(val expr: ExpressionTree, val path: TreePath) { init { require(path.leaf == expr) } }

object Expr {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val locatedExpression: LocatedExpression,
        val assign: Boolean,
        val program: Program
    ) {
        val expression get() = locatedExpression.expr
        val path get() = locatedExpression.path
    }
    data class Right(
        val tc: TC,
        val fields: TypeEnv,
        val variables: TypeEnv
    ) {
        constructor(tc: TC, delta: Delta) : this(tc, delta.fields, delta.variables)
    }
}

val typingExpression: Judgement<Expr.Left, Expr.Right> = judgement {

    rule("TVal") {
        premise { typingValue.derive(Value(locatedExpression, program)) }
        conclusion {
            left { expression is LiteralTree || expression is MemberSelectTree }
            right { Expr.Right(it, fields, variables) }
        }
    }

    rule<TNew>("TNew") {
        premise {
            val newExpr = expression as NewClassTree
            val c = program.asJavaClass(TreePath(path, newExpr.identifier)) ?: fail()
            ensure(assign || c.protocol?.let { term(U(it.initState)) } ?: true)
            val args = newExpr.arguments.map { LocatedExpression(it, TreePath(path, it)) }
            val exprSeqL = ExprSeq.Left(fields, variables, args, true, program)
            val exprSeqR = typingExpressionSequence.derive(exprSeqL)
            val constructor = program.asJavaConstructor(path) ?: fail()
            ensure(exprSeqR.tcs.zip(constructor.pt).all { (tc, pt) -> tc sub toTC(pt) })
            TNew(c, exprSeqR.fields to exprSeqR.variables)
        }
        conclusion {
            left { expression is NewClassTree }
            right { Expr.Right(tt(it.c, it.c.protocol?.let { U(it.initState) } ?: Shared), it.delta) }
        }
    }

    rule<TUpdB>("TUpdB") {
        premise {
            val c = (variables["this"] as TypeStateTree).clazz as JavaClass
            val assignment = expression as AssignmentTree
            val e = LocatedExpression(assignment.expression, TreePath(path, assignment.expression))
            val exprL = Expr.Left(fields, variables, e, true, program)
            val exprR = typingExpression.derive(exprL)
            ensure(exprR.tc !is TypeStateTree)
            val eid = when (val variable = assignment.variable) {
                is IdentifierTree -> Eid(variable.name.toString(), Receiver.NONE)
                is MemberSelectTree -> when ((variable.expression as IdentifierTree).name.toString()) {
                    "this" -> Eid(variable.identifier.toString(), Receiver.THIS)
                    "super" -> Eid(variable.identifier.toString(), Receiver.SUPER)
                    else -> fail()
                }
                else -> fail()
            }
            val lkp = lookup(c, eid, exprR.fields to exprR.variables) ?: fail()
            ensure(lkp in listOf(Bool, BoolUnd, Integer, IntegerUnd, Double, DoubleUnd) || lkp is EnumType)
            val toTC = when (lkp) {
                Bool, BoolUnd -> Bool
                Integer, IntegerUnd -> Integer
                Double, DoubleUnd -> Double
                is EnumType -> lkp.copy(und = false)
                else -> fail()
            }
            ensure(exprR.tc sub toTC)
            TUpdB(c, eid, exprR.tc, exprR.fields to exprR.variables)
        }
        conclusion {
            left {
                val variable = (expression as? AssignmentTree)?.variable
                variable is IdentifierTree || variable is MemberSelectTree &&
                        (variable.expression as? IdentifierTree)
                            ?.name
                            ?.toString() in setOf("this", "super")
            }
            right { Expr.Right(it.tc, upd(it.c, it.eid, it.tc, it.delta)) }
        }
    }
}