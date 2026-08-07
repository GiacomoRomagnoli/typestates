package rules

import com.sun.source.tree.ExpressionStatementTree
import com.sun.source.tree.VariableTree
import com.sun.source.util.TreePath
import language.model.JavaClass
import language.model.Program
import language.model.isSubClassOf
import language.types.Delta
import language.types.Eid
import language.types.RT
import language.types.TypeEnv
import language.types.TypeStateTree
import language.types.fields
import language.types.get
import language.types.resolve
import language.types.ucastTT
import language.types.upd
import language.types.variables
import rules.dsl.Judgement
import rules.dsl.judgement
import rules.utils.toEid

private data class TVInitO(val c: JavaClass, val id: Eid, val tt1: TypeStateTree, val delta: Delta)

object Stmt {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val breakFields: TypeEnv,
        val breakVariables: TypeEnv,
        val returnFields: TypeEnv,
        val path: TreePath,
        val returnType: RT,
        val program: Program,
        val f: Boolean = false
    ) {
        val statement get() = path.leaf
    }

    data class Right(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val breakFields: TypeEnv,
        val breakVariables: TypeEnv,
        val returnFields: TypeEnv,
    )
}

private fun Stmt.Left.withDelta(delta: Delta) =
    Stmt.Right(
        delta.fields,
        delta.variables,
        breakFields,
        breakVariables,
        returnFields
    )

val STATEMENT_JUDGMENT: Judgement<Stmt.Left, Stmt.Right> = judgement {

    rule<Delta>("TExp") {
        premise {
            val stmt = statement as ExpressionStatementTree
            val expr = TreePath(path, stmt.expression)
            val result = EXPRESSION_JUDGMENT.derive(
                Expr.Left(fields, variables, expr, false, program)
            )
            result.fields to result.variables
        }
        conclusion {
            left { statement is ExpressionStatementTree }
            right { withDelta(resolve(it.fields) to resolve(it.variables)) }
        }
    }

    rule<TVInitO>("TVInitO") {
        premise {
            val statement = statement as VariableTree
            val expression = TreePath(path, statement.initializer)
            val c = (variables["this"] as TypeStateTree).clazz as JavaClass
            val exprDerivation = EXPRESSION_JUDGMENT.derive(
                Expr.Left(fields, variables, expression, true, program)
            )
            val tt = exprDerivation.tc as? TypeStateTree ?: fail()
            val declaration = TreePath(path, statement.type)
            val c1 = program.classByPath(declaration) ?: fail()
            ensure(tt.clazz isSubClassOf c1)
            val tt1 = ucastTT(tt, c1)
            val declDerivation = VARIABLE_DECLARATION_JUDGEMENT.derive(
                VarDecl.Left(
                    exprDerivation.fields,
                    exprDerivation.variables,
                    declaration,
                    statement.name.toString(),
                    f,
                    program,
                )
            )
            TVInitO(c, statement.name.toEid(), tt1, declDerivation.fields to declDerivation.variables)
        }
        conclusion {
            left {
                !f && (statement as? VariableTree)
                    ?.takeIf { it.initializer != null }
                    ?.let { program.classByPath(TreePath(path, it.type)) } != null
            }
            right { withDelta(upd(it.c, it.id, it.tt1, it.delta)) }
        }
    }
}