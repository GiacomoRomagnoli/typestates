package rules

import com.sun.source.util.TreePath
import language.model.Program
import language.types.TypeEnv
import rules.dsl.judgement

// TODO aggiungere ambienti
object VarDecl {
    data class Left(
        val fields: TypeEnv,
        val variables: TypeEnv,
        val type: TreePath,
        val id: String,
        val f: Boolean,
        val program: Program,
    )

    data class Right(
        val fields: TypeEnv,
        val variables: TypeEnv,
    )
}

val VARIABLE_DECLARATION_JUDGEMENT = judgement<VarDecl.Left, VarDecl.Right> {
    // TODO aggiungere regole: TVDeclO, TVDeclB, TVDeclF
}