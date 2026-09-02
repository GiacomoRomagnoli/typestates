package rules

import com.sun.source.tree.BlockTree
import com.sun.source.tree.ExpressionStatementTree
import com.sun.source.tree.IfTree
import com.sun.source.tree.PrimitiveTypeTree
import com.sun.source.tree.ReturnTree
import com.sun.source.tree.VariableTree
import com.sun.source.tree.WhileLoopTree
import com.sun.source.util.TreePath
import language.model.JavaClass
import language.model.Program
import language.model.isSubClassOf
import language.types.Bool
import language.types.Delta
import language.types.Double
import language.types.Eid
import language.types.EnumType
import language.types.Integer
import language.types.RT
import language.types.TC
import language.types.THIS
import language.types.TypeEnv
import language.types.TypeStateTree
import language.types.bottom
import language.types.Tf
import language.types.merge
import language.types.resolve
import language.types.sub
import language.types.term
import language.types.toTC
import language.types.ucastTT
import language.types.upd
import language.types.Ts
import language.types.evolve
import rules.dsl.Judgement
import rules.dsl.Traceable
import rules.dsl.judgement
import rules.utils.toEid
import javax.lang.model.type.TypeKind

private data class TVInit(
    val c: JavaClass,
    val id: Eid,
    val tc: TC,
    val delta: Delta,
    val bf: TypeEnv,
    val bs: TypeEnv,
    val ret: TypeEnv
)
private data class TIf(val t: Stmt.Right, val f: Stmt.Right)
private data class TWhl(val e: Expr.Right, val st: Stmt.Right)

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
    ): Traceable {
        val stmt get() = path.leaf
        override fun trace(): String = stmt.toString()
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
        delta.Tf,
        delta.Ts,
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
            right { withDelta(resolve(it.Tf) to resolve(it.Ts)) }
        }
    }

    rule<TVInit>("TVInitO") {
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
            TVInit(c, statement.name.toEid(), tt1, declJdg.Tf to declJdg.Ts, declJdg.Tbf, declJdg.Tbs, declJdg.Tret,)
        }
        side { program.classByPath(TreePath(path, (stmt as VariableTree).type)) != null }
        conclusion {
            left { (stmt as? VariableTree)?.initializer != null }
            right {
                val upd = upd(it.c, it.id, it.tc, it.delta)
                Stmt.Right(upd.Tf, upd.Ts, it.bf, it.bs, it.ret)
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

    rule<TVInit>("TVInitB") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val declaration = stmt as VariableTree
            val e = TreePath(path, declaration.initializer)
            val eJdg = EXPRESSION_JUDGMENT.derive(Expr.Left(Tf, Ts, e, true, program))
            val jtPath = TreePath(path, declaration.type)
            val jt = when(val t = declaration.type) {
                is PrimitiveTypeTree -> when(t.primitiveTypeKind) {
                    TypeKind.BOOLEAN -> Bool
                    TypeKind.INT -> Integer
                    TypeKind.DOUBLE -> Double
                    else -> fail()
                }
                else -> program.enumByTypePath(jtPath)
                    ?.let(::EnumType)
                    ?: fail()
            }
            ensure(eJdg.tc sub jt)
            val declJdg = VARIABLE_DECLARATION_JUDGEMENT.derive(
                VarDecl.Left(
                    resolve(eJdg.Tf), resolve(eJdg.Ts),
                    Tbf, Tbs, Tret, program, f, jtPath, declaration.name.toString()
                )
            )
            ensure(Tret == declJdg.Tret)
            TVInit(c, declaration.name.toEid(), eJdg.tc, declJdg.Tf to declJdg.Ts, declJdg.Tbf, declJdg.Tbs, declJdg.Tret,)
        }
        side { program.classByPath(TreePath(path, (stmt as VariableTree).type)) == null }
        conclusion {
            left { (stmt as? VariableTree)?.initializer != null }
            right {
                val upd = upd(it.c, it.id, it.tc, it.delta)
                Stmt.Right(upd.Tf, upd.Ts, it.bf, it.bs, it.ret)
            }
        }
    }

    rule("TBlock") {
        premise {
            val block = stmt as BlockTree
            val bst = block.statements.map { TreePath(path, it) }
            val bstJdg = STATEMENT_SEQUENCE_JUDGMENT.derive(
                StmtSeq.Left(Tf, Ts, Tbf, Tbs, Tret, rt, program, false, bst)
            )
            ensure(term(bstJdg.Ts - Ts.keys))
            ensure(term(bstJdg.Tbs - Ts.keys))
            val Ts2 = bstJdg.Ts.filterKeys { Ts.containsKey(it) }
            val Tbs2 = bstJdg.Tbs.filterKeys { Ts.containsKey(it) }
            Stmt.Right(bstJdg.Tf, Ts2, bstJdg.Tbf, Tbs2, bstJdg.Tret)
        }
        conclusion {
            left { !f && stmt is BlockTree }
            right { it }
        }
    }

    rule<TIf>("TIf") {
        premise {
            val ifStmt = stmt as IfTree
            val e = TreePath(path, ifStmt.condition)
            val eJdg = EXPRESSION_JUDGMENT.derive(Expr.Left(Tf, Ts, e, false, program))
            ensure(eJdg.tc is Bool)
            val trueJdg = STATEMENT_JUDGMENT.derive(
                copy(
                    Tf = evolve(eJdg.Tf, Bool.TRUE),
                    Ts = evolve(eJdg.Ts, Bool.TRUE),
                    path = TreePath(path, ifStmt.thenStatement)
                )
            )
            val falseJdg = STATEMENT_JUDGMENT.derive(
                copy(
                    Tf = evolve(eJdg.Tf, Bool.FALSE),
                    Ts = evolve(eJdg.Ts, Bool.FALSE),
                    path = TreePath(path, ifStmt.elseStatement ?: fail())
                )
            )
            TIf(trueJdg, falseJdg)
        }
        conclusion {
            left { !f && stmt is IfTree }
            right {
                Stmt.Right(
                    merge(it.t.Tf, it.f.Tf),
                    merge(it.t.Ts, it.f.Ts),
                    merge(it.t.Tbf, it.f.Tbf),
                    merge(it.t.Tbs, it.f.Tbs),
                    merge(it.t.Tret, it.f.Tret),
                )
            }
        }
    }

    rule<TWhl>("TWhl") {
        premise {
            val loop = stmt as WhileLoopTree
            val e = TreePath(path, loop.condition)
            val eJdg = EXPRESSION_JUDGMENT.derive(Expr.Left(Tf, Ts, e, false, program))
            ensure(eJdg.tc is Bool)
            val stJdg = STATEMENT_JUDGMENT.derive(
                copy(
                    Tf = evolve(eJdg.Tf, Bool.TRUE),
                    Ts = evolve(eJdg.Ts, Bool.TRUE),
                    path = TreePath(path, loop.statement)
                )
            )
            ensure(Ts.keys.all { stJdg.Ts[it]?.sub(Ts[it]!!) ?: false })
            ensure(stJdg.Tf.keys.all { stJdg.Tf[it]!! sub (Tf[it] ?: return@all false) })
            TWhl(eJdg, stJdg)
        }
        conclusion {
            left { !f && stmt is WhileLoopTree }
            right {
                Stmt.Right(
                    merge(evolve(it.e.Tf, Bool.FALSE), it.st.Tbf),
                    merge(evolve(it.e.Ts, Bool.FALSE), it.st.Tbs),
                    Tbf, Tbs,
                    merge(Tret, it.st.Tret)
                )
            }
        }
    }
}