package rules

import com.sun.source.util.TreePath
import language.model.Program
import language.types.BottomTC
import language.types.Id
import language.types.get
import language.types.TypeEnv
import language.types.Und
import language.types.tt
import rules.dsl.judgement

object VarDecl {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val breakFields: TypeEnv,
        val breakVariables: TypeEnv,
        val returnFields: TypeEnv,
        val type: TreePath,
        val id: String,
        val f: Boolean,
        val program: Program,
    )

    data class Right(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val breakFields: TypeEnv,
        val breakVariables: TypeEnv,
        val returnFields: TypeEnv,
    )
}

val VARIABLE_DECLARATION_JUDGEMENT = judgement<VarDecl.Left, VarDecl.Right> {
    // TODO aggiungere regole: TVDeclB, TVDeclF
    rule("TVDeclO") {
        premise {
            ensure(variables[id] == null)
            program.classByPath(type)!!
        }
        conclusion {
            left { program.classByPath(type) != null }
            right {
                VarDecl.Right(
                    fields,
                    variables + (Id(id) to tt(it, Und)),
                    breakFields,
                    breakVariables + (Id(id) to BottomTC),
                    returnFields
                )
            }
        }
    }
}