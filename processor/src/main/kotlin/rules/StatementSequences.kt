package rules

import com.sun.source.util.TreePath
import language.model.Program
import language.types.TypeEnv
import rules.dsl.Judgement
import rules.dsl.judgement

object StmtSeq {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val statements: List<TreePath>,
        val program: Program,
    )
}

val STATEMENT_SEQUENCE_JUDGMENT: Judgement<StmtSeq.Left, Stmt.Right> = judgement {

    rule("TEmpty") {
        premise {  }
        conclusion {
            left { statements.isEmpty() }
            right {
                Stmt.Right(fields, variables)
            }
        }
    }

    rule("TSeqSt") {
        premise {
            val head = STATEMENT_JUDGMENT.derive(
                Stmt.Left(fields, variables, statements.first(), program)
            )
            STATEMENT_SEQUENCE_JUDGMENT.derive(
                copy(fields = head.fields, variables = head.variables, statements = statements.drop(1))
            )
        }
        conclusion {
            left { statements.isNotEmpty() }
            right { it }
        }
    }
}