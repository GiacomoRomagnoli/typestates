package rules

import com.sun.source.util.TreePath
import language.model.Program
import language.types.RT
import language.types.TypeEnv
import rules.dsl.Judgement
import rules.dsl.Traceable
import rules.dsl.judgement

object StmtSeq {
    data class Left(
        val Tf: TypeEnv,
        val Ts: TypeEnv,
        val Tbf: TypeEnv,
        val Tbs: TypeEnv,
        val Tret: TypeEnv,
        val rt: RT,
        val program: Program,
        val f: Boolean,
        val stmts: List<TreePath>,
    ) : Traceable {
        override fun trace(): String =
            if (stmts.isEmpty()) "<empty>" else "${stmts.size} statements, next: ${stmts.first().leaf}"
    }
}

val STATEMENT_SEQUENCE_JUDGMENT: Judgement<StmtSeq.Left, Stmt.Right> = judgement {

    rule("TEmpty") {
        premise {  }
        conclusion {
            left { stmts.isEmpty() }
            right { Stmt.Right(Tf, Ts, Tbf, Tbs, Tret) }
        }
    }

    rule("TSeqSt") {
        premise {
            val head = STATEMENT_JUDGMENT.derive(
                Stmt.Left(Tf, Ts, Tbf, Tbs, Tret, program, rt, f,stmts.first())
            )
            STATEMENT_SEQUENCE_JUDGMENT.derive(
                copy(
                    Tf = head.Tf,
                    Ts = head.Ts,
                    Tbf = head.Tbf,
                    Tbs = head.Tbs,
                    Tret = head.Tret,
                    stmts = stmts.drop(1)
                )
            )
        }
        conclusion {
            left { stmts.isNotEmpty() }
            right { it }
        }
    }
}