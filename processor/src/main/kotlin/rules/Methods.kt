package rules

import com.sun.source.tree.BlockTree
import com.sun.source.util.TreePath
import language.model.JavaClass
import language.model.JavaMethod
import language.model.Program
import language.types.Shared
import language.types.THIS
import language.types.TypeEnv
import language.types.Void
import language.types.bottom
import language.types.term
import language.types.toTC
import language.types.tt
import rules.dsl.Judgement
import rules.dsl.judgement

object Meth {
    data class Left(
        val Tf: TypeEnv,
        val method: JavaMethod,
        val program: Program,
        val c: JavaClass
    )
}

val METHOD_JUDGEMENT: Judgement<Meth.Left, TypeEnv> = judgement {
    rule("TMeth1") {
        premise {
            val Ts: TypeEnv = buildMap {
                put(THIS, tt(c, Shared))
                method.pt.forEach { put(it.name, toTC(it.type)) }
            }
            val Tbf = Tf.bottom()
            val Tret = Tf.bottom()
            val Tbs = Ts.bottom()
            val body = method.body ?: fail()
            val stmts = (body.leaf as BlockTree).statements.map { TreePath(body, it) }
            val jdg = STATEMENT_SEQUENCE_JUDGMENT.derive(
                StmtSeq.Left(Tf, Ts, Tbf, Tbs, Tret, Void, program, false, stmts)
            )
            ensure(jdg.Tbf == Tbf)
            ensure(jdg.Tret == Tret)
            ensure(jdg.Tbs == jdg.Tbs.bottom())
            ensure(term(jdg.Ts))
            jdg.Tf
        }
        conclusion {
            left { method.rt == Void }
            right { it }
        }
    }

    rule("TMeth2") {
        premise {
            val Ts: TypeEnv = buildMap {
                put(THIS, tt(c, Shared))
                method.pt.forEach { put(it.name, toTC(it.type)) }
            }
            val Tbf = Tf.bottom()
            val Tret = Tf.bottom()
            val Tbs = Ts.bottom()
            val body = method.body ?: fail()
            val stmts = (body.leaf as BlockTree).statements.map { TreePath(body, it) }
            val jdg = STATEMENT_SEQUENCE_JUDGMENT.derive(
                StmtSeq.Left(Tf, Ts, Tbf, Tbs, Tret, method.rt, program, false, stmts,)
            )
            ensure(jdg.Tf == jdg.Tf.bottom())
            ensure(jdg.Ts == jdg.Ts.bottom())
            ensure(jdg.Tbf == Tbf)
            ensure(jdg.Tbs == jdg.Tbs.bottom())
            jdg.Tret
        }
        conclusion {
            left { method.rt != Void}
            right { it }
        }
    }
}