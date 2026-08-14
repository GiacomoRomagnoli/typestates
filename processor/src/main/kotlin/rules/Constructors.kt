package rules

import com.sun.source.tree.BlockTree
import com.sun.source.util.TreePath
import language.model.JavaConstructor
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
import rules.utils.isSuperCall

data class Cns(
    val constructor: JavaConstructor,
    val program: Program,
)

val CONSTRUCTOR_JUDGMENT: Judgement<Cns, TypeEnv> =
    judgement {

        rule("TCns") {
            premise {
                var ts: TypeEnv = mapOf(THIS to tt(constructor.owner, Shared))
                val fields = constructor.owner.fields.map { it.statement ?: fail() }
                val fieldR = STATEMENT_SEQUENCE_JUDGMENT.derive(
                    StmtSeq.Left(
                        emptyMap(), ts, emptyMap(),
                        emptyMap(), emptyMap(),
                        Void, program, true,
                        fields
                    )
                )
                val tf = fieldR.Tf
                val tbf = tf.bottom()
                val tret = tf.bottom()
                ts = ts + constructor.pt.map { it.name to toTC(it.type) }
                val tbs = ts.bottom()
                val bodyPath = constructor.body ?: fail()
                val body = bodyPath.leaf as? BlockTree ?: fail()
                val stmts = body.statements
                    .drop(if (body.statements.firstOrNull()?.isSuperCall() == true) 1 else 0)
                    .map { TreePath(bodyPath, it) }
                val bodyR = STATEMENT_SEQUENCE_JUDGMENT.derive(
                    StmtSeq.Left(tf, ts, tbf, tbs, tret, Void, program, false, stmts)
                )
                ensure(bodyR.Tbf == tbf)
                ensure(bodyR.Tret == tret)
                ensure(bodyR.Tbs == bodyR.Tbs.bottom())
                ensure(term(bodyR.Ts))
                bodyR.Tf
            }
            conclusion {
                left { constructor.owner.superclass?.qualifiedName == "java.lang.Object" }
                right { it }
            }
        }

    }