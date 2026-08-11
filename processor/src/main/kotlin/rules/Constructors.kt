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
                        emptyMap(), ts, fields,
                        emptyMap(), emptyMap(), emptyMap(),
                        Void, program, f = true
                    )
                )
                val tf = fieldR.fields
                val tbf = tf.bottom()
                val tret = tf.bottom()
                ts = ts + constructor.pt.map { it.name to toTC(it.type) }
                val tbs = ts.bottom()
                val bodyPath = constructor.body ?: fail()
                val body = bodyPath.leaf as? BlockTree ?: fail()
                val statements = body.statements
                    .drop(if (body.statements.firstOrNull()?.isSuperCall() == true) 1 else 0)
                    .map { TreePath(bodyPath, it) }
                val bodyR = STATEMENT_SEQUENCE_JUDGMENT.derive(
                    StmtSeq.Left(
                        tf, ts, statements,
                        tbf, tbs, tret,
                        Void, program, f = false
                    )
                )
                ensure(bodyR.breakFields == tbf)
                ensure(bodyR.returnFields == tret)
                ensure(bodyR.breakVariables == bodyR.breakVariables.bottom())
                ensure(term(bodyR.variables))
                bodyR.fields
            }
            conclusion {
                left { constructor.owner.superclass?.qualifiedName == "java.lang.Object" }
                right { it }
            }
        }

    }