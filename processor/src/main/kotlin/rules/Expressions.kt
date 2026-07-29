package rules

import com.sun.source.tree.AssignmentTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.NewClassTree
import com.sun.source.util.TreePath
import language.model.JavaClass
import language.model.Program
import language.model.isSubClassOf
import language.types.Bool
import language.types.BoolUnd
import language.types.Delta
import language.types.Double
import language.types.DoubleUnd
import language.types.Eid
import language.types.EnumType
import language.types.Integer
import language.types.IntegerUnd
import language.types.Shared
import language.types.TC
import language.types.TypeEnv
import language.types.TypeStateTree
import language.types.get
import language.types.U
import language.types.alias
import language.types.defined
import language.types.fields
import language.types.lookup
import language.types.sub
import language.types.term
import language.types.toTC
import language.types.tt
import language.types.ucastTT
import language.types.upd
import language.types.variables
import rules.dsl.Judgement
import rules.dsl.judgement
import rules.utils.toEid

/**
 * data classes for partial result of rules
 */
data class TNew(val c: JavaClass, val delta: Delta)
data class TUpdB(val c: JavaClass, val eid: Eid, val tc: TC, val delta: Delta)
data class TUpdO(val c: JavaClass, val eid: Eid, val tt1: TypeStateTree, val tt2: TypeStateTree, val delta: Delta)

object Expr {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val path: TreePath,
        val assign: Boolean,
        val program: Program
    ) {
        val expression get() = path.leaf
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
        premise { typingValue.derive(Value(path, program)) }
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
            val args = newExpr.arguments.map { TreePath(path, it) }
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
            val e = TreePath(path, assignment.expression)
            val exprL = Expr.Left(fields, variables, e, true, program)
            val exprR = typingExpression.derive(exprL)
            ensure(exprR.tc !is TypeStateTree)
            val eid = assignment.variable.toEid() ?: fail()
            val lkp = lookup(c, eid, exprR.fields to exprR.variables) ?: fail()
            ensure(lkp in listOf(Bool, BoolUnd, Integer, IntegerUnd, Double, DoubleUnd) || lkp is EnumType)
            ensure(exprR.tc sub lkp.defined())
            TUpdB(c, eid, exprR.tc, exprR.fields to exprR.variables)
        }
        conclusion {
            left { (expression as? AssignmentTree)?.variable?.toEid() != null }
            right { Expr.Right(it.tc, upd(it.c, it.eid, it.tc, it.delta)) }
        }
    }

    rule<TUpdO>("TUpdO") {
        premise {
            val c = (variables["this"] as TypeStateTree).clazz as JavaClass
            val assignment = expression as AssignmentTree
            val e = TreePath(path, assignment.expression)
            val exprL = Expr.Left(fields, variables, e, true, program)
            val exprR = typingExpression.derive(exprL)
            val tt1 = exprR.tc as? TypeStateTree ?: fail()
            val eid = assignment.variable.toEid() ?: fail()
            val delta = exprR.fields to exprR.variables
            val tt = lookup(c, eid, delta) as? TypeStateTree ?: fail()
            ensure(term(tt))
            ensure(tt1.clazz isSubClassOf tt.clazz)
            val cl = tt.clazz as? JavaClass ?: fail()
            val tt2 = ucastTT(tt1, cl)
            TUpdO(c, eid, tt1, tt2, delta)
        }
        conclusion {
            left { (expression as? AssignmentTree)?.variable?.toEid() != null }
            right { Expr.Right(alias(it.tt1), upd(it.c, it.eid, it.tt2, it.delta)) }
        }
    }
}