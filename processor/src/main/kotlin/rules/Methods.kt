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
        val fields: TypeEnv,
        val method: JavaMethod,
        val program: Program,
        val c: JavaClass
    )
}

val METHOD_JUDGEMENT: Judgement<Meth.Left, TypeEnv> = judgement {
    rule("TMeth1") {
        premise {
            val variables: TypeEnv = buildMap {
                put(THIS, tt(c, Shared))
                method.pt.forEach { put(it.name, toTC(it.type)) }
            }
            val breakFields = fields.bottom()
            val returnFields = fields.bottom()
            val breakVariables = variables.bottom()
            val body = method.body ?: fail()
            val statements = (body.leaf as BlockTree).statements.map { TreePath(body, it) }
            val derivation = STATEMENT_SEQUENCE_JUDGMENT.derive(
                StmtSeq.Left(
                    fields = fields,
                    variables = variables,
                    breakFields = breakFields,
                    breakVariables = breakVariables,
                    returnFields = returnFields,
                    statements = statements,
                    returnType = Void,
                    program = program,
                    f = false
                )
            )
            ensure(derivation.breakFields == breakFields)
            ensure(derivation.returnFields == returnFields)
            ensure(derivation.breakVariables == derivation.breakVariables.bottom())
            ensure(term(derivation.variables))
            derivation.fields
        }
        conclusion {
            left { method.rt == Void }
            right { it }
        }
    }

    rule("TMeth2") {
        premise {
            val variables: TypeEnv = buildMap {
                put(THIS, tt(c, Shared))
                method.pt.forEach { put(it.name, toTC(it.type)) }
            }
            val breakFields = fields.bottom()
            val returnFields = fields.bottom()
            val breakVariables = variables.bottom()
            val body = method.body ?: fail()
            val statements = (body.leaf as BlockTree).statements.map { TreePath(body, it) }
            val derivation = STATEMENT_SEQUENCE_JUDGMENT.derive(
                StmtSeq.Left(
                    fields = fields,
                    variables = variables,
                    breakFields = breakFields,
                    breakVariables = breakVariables,
                    returnFields = returnFields,
                    statements = statements,
                    returnType = method.rt,
                    program = program,
                    f = false
                )
            )
            ensure(derivation.fields == derivation.fields.bottom())
            ensure(derivation.variables == derivation.variables.bottom())
            ensure(derivation.breakFields == breakFields)
            ensure(derivation.breakVariables == derivation.breakVariables.bottom())
            derivation.returnFields
        }
        conclusion {
            left { method.rt != Void}
            right { it }
        }
    }
}