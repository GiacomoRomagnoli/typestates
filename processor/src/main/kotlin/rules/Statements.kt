package rules

import com.sun.source.tree.ExpressionStatementTree
import com.sun.source.tree.ReturnTree
import com.sun.source.tree.VariableTree
import com.sun.source.util.TreePath
import language.model.JavaClass
import language.model.Program
import language.model.isSubClassOf
import language.types.Delta
import language.types.Eid
import language.types.RT
import language.types.THIS
import language.types.TypeEnv
import language.types.TypeStateTree
import language.types.bottom
import language.types.fields
import language.types.merge
import language.types.resolve
import language.types.sub
import language.types.term
import language.types.toTC
import language.types.ucastTT
import language.types.upd
import language.types.variables
import rules.dsl.Judgement
import rules.dsl.judgement
import rules.utils.toEid

private data class TVInitO(
    val c: JavaClass,
    val id: Eid,
    val tt1: TypeStateTree,
    val delta: Delta,
    val bf: TypeEnv,
    val bs: TypeEnv,
    val ret: TypeEnv
)

object Stmt {
    data class Left(
        val Tf: TypeEnv,
        val Ts: TypeEnv,
        val Tbf: TypeEnv,
        val Tbs: TypeEnv,
        val Tret: TypeEnv,
        val program: Program,
        val rt: RT,
        val f: Boolean = false,
        val path: TreePath,
    ) {
        val stmt get() = path.leaf
    }

    data class Right(
        val Tf: TypeEnv,
        val Ts: TypeEnv,
        val Tbf: TypeEnv,
        val Tbs: TypeEnv,
        val Tret: TypeEnv,
    )
}

private fun Stmt.Left.withDelta(delta: Delta) =
    Stmt.Right(
        delta.fields,
        delta.variables,
        Tbf,
        Tbs,
        Tret
    )

val STATEMENT_JUDGMENT: Judgement<Stmt.Left, Stmt.Right> = judgement {

    rule<Delta>("TExp") {
        premise {
            val stmt = stmt as ExpressionStatementTree
            val expr = TreePath(path, stmt.expression)
            val result = EXPRESSION_JUDGMENT.derive(
                Expr.Left(Tf, Ts, expr, false, program)
            )
            result.Tf to result.Ts
        }
        conclusion {
            left { stmt is ExpressionStatementTree }
            right { withDelta(resolve(it.fields) to resolve(it.variables)) }
        }
    }

    rule<TVInitO>("TVInitO") {
        premise {
            val statement = stmt as VariableTree
            val expression = TreePath(path, statement.initializer)
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val eJdg = EXPRESSION_JUDGMENT.derive(Expr.Left(Tf, Ts, expression, true, program))
            val tt = eJdg.tc as? TypeStateTree ?: fail()
            val jt = TreePath(path, statement.type)
            val c1 = program.classByPath(jt) ?: fail()
            ensure(tt.clazz isSubClassOf c1)
            val tt1 = ucastTT(tt, c1)
            val declJdg = VARIABLE_DECLARATION_JUDGEMENT.derive(
                VarDecl.Left(eJdg.Tf, eJdg.Ts, Tbf, Tbs, Tret, program, f, jt, statement.name.toString())
            )
            ensure(Tret == declJdg.Tret)
            TVInitO(c, statement.name.toEid(), tt1, declJdg.Tf to declJdg.Ts, declJdg.Tbf, declJdg.Tbs, declJdg.Tret,)
        }
        conclusion {
            left {
                (stmt as? VariableTree)
                    ?.takeIf { it.initializer != null }
                    ?.let { program.classByPath(TreePath(path, it.type)) } != null
            }
            right {
                val upd = upd(it.c, it.id, it.tt1, it.delta)
                Stmt.Right(upd.fields, upd.variables, it.bf, it.bs, it.ret)
            }
        }
    }

    rule<VarDecl.Right>("TVDecl") {
        premise {
            val stmt = stmt as VariableTree
            val jt = TreePath(path, stmt.type)
            VARIABLE_DECLARATION_JUDGEMENT.derive(
                VarDecl.Left(Tf, Ts, Tbf, Tbs, Tret, program, f, jt, stmt.name.toString())
            )
        }
        conclusion {
            left { stmt is VariableTree && (stmt as VariableTree).initializer == null }
            right { Stmt.Right(it.Tf, it.Ts, it.Tbf, it.Tbs, it.Tret) }
        }
    }

    rule("TRet") {
        premise {
            val ret = stmt as ReturnTree
            val e = TreePath(path, ret.expression)
            val jdg = EXPRESSION_JUDGMENT.derive(Expr.Left(Tf, Ts, e, true, program))
            ensure(jdg.tc sub toTC(rt))
            ensure(term(resolve(jdg.Ts)))
            jdg.Tf
        }
        conclusion {
            left { stmt is ReturnTree && (stmt as ReturnTree).expression != null }
            right { Stmt.Right(Tf.bottom(), Ts.bottom(), Tbf, Tbs, merge(Tret, it)) }
        }
    }
}