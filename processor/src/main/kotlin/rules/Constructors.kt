package rules

import com.sun.source.tree.BlockTree
import com.sun.source.util.TreePath
import language.model.JavaConstructor
import language.model.Program
import language.types.Shared
import language.types.THIS
import language.types.Tf
import language.types.Ts
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

        rule("TCnsExt1") {
            premise {
                val ts: TypeEnv = mapOf(THIS to tt(constructor.owner, Shared))
                val ts1 = ts + constructor.pt.map { it.name to toTC(it.type) }
                val bodyPath = constructor.body ?: fail()
                val body = bodyPath.leaf as? BlockTree ?: fail()
                val call = TreePath(bodyPath, body.statements.first())
                val superJdg = SUPER_CALL_JUDGEMENT.derive(SuperCall.Left(ts1, program, call))
                val fields = constructor.owner.fields.map { it.statement ?: fail() }
                val fieldsJdg = STATEMENT_SEQUENCE_JUDGMENT.derive(
                    StmtSeq.Left(
                        superJdg.Tf, ts, emptyMap(),
                        emptyMap(), emptyMap(),
                        Void, program, true,
                        fields
                    )
                )
                val tbf = fieldsJdg.Tf.bottom()
                val tret = fieldsJdg.Tf.bottom()
                val tbs = superJdg.Ts.bottom()
                val stmts = body.statements.drop(1).map { TreePath(bodyPath, it) }
                val stmtsJdg = STATEMENT_SEQUENCE_JUDGMENT.derive(
                    StmtSeq.Left(fieldsJdg.Tf, superJdg.Ts, tbf, tbs, tret, Void, program, false, stmts)
                )
                ensure(stmtsJdg.Tbf == tbf)
                ensure(stmtsJdg.Tret == tret)
                ensure(stmtsJdg.Tbs == stmtsJdg.Tbs.bottom())
                ensure(term(stmtsJdg.Ts))
                stmtsJdg.Tf
            }
            conclusion {
                left {
                    constructor.owner.superclass != null &&
                        constructor.owner.superclass!!.qualifiedName != "java.lang.Object" &&
                            (constructor.body?.leaf as? BlockTree)?.statements?.firstOrNull()?.isSuperCall() == true
                }
                right { it }
            }
        }

        rule("TCnsExt2") {
            premise {
                val ts: TypeEnv = mapOf(THIS to tt(constructor.owner, Shared))
                val ts1 = ts + constructor.pt.map { it.name to toTC(it.type) }
                val superClass = constructor.owner.superclass ?: fail()
                val superConstructor = superClass.constructors.firstOrNull { it.pt.isEmpty() } ?: fail()
                val bodyPath = constructor.body ?: fail()
                val body = bodyPath.leaf as? BlockTree ?: fail()
                val stmts = body.statements.map { TreePath(bodyPath, it) }
                val superTf = CONSTRUCTOR_JUDGMENT.derive(Cns(superConstructor, program))
                val fields = constructor.owner.fields.map { it.statement ?: fail() }
                val fieldsJdg = STATEMENT_SEQUENCE_JUDGMENT.derive(
                    StmtSeq.Left(
                        superTf, ts,
                        emptyMap(), emptyMap(), emptyMap(),
                        Void, program, true, fields
                    )
                )
                val tbf = fieldsJdg.Tf.bottom()
                val tret = fieldsJdg.Tf.bottom()
                val tbs = ts1.bottom()
                val stmtsJdg = STATEMENT_SEQUENCE_JUDGMENT.derive(
                    StmtSeq.Left(
                        fieldsJdg.Tf, ts1,
                        tbf, tbs, tret,
                        Void, program, false, stmts
                    )
                )
                ensure(stmtsJdg.Tbf == tbf)
                ensure(stmtsJdg.Tret == tret)
                ensure(stmtsJdg.Tbs == stmtsJdg.Tbs.bottom())
                ensure(term(stmtsJdg.Ts))
                stmtsJdg.Tf
            }
            conclusion {
                left {
                    constructor.owner.superclass != null &&
                            constructor.owner.superclass!!.qualifiedName != "java.lang.Object" &&
                            (constructor.body?.leaf as? BlockTree)
                                ?.statements
                                ?.firstOrNull()
                                ?.isSuperCall() != true
                }
                right { it }
            }
        }
    }