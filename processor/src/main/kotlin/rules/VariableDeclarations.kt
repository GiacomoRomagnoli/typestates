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
        val Tf: TypeEnv,
        val Ts: TypeEnv,
        val Tbf: TypeEnv,
        val Tbs: TypeEnv,
        val Tret: TypeEnv,
        val program: Program,
        val f: Boolean,
        val jt: TreePath,
        val id: String,
    )

    data class Right(
        val Tf: TypeEnv,
        val Ts: TypeEnv,
        val Tbf: TypeEnv,
        val Tbs: TypeEnv,
        val Tret: TypeEnv,
    )
}

val VARIABLE_DECLARATION_JUDGEMENT = judgement<VarDecl.Left, VarDecl.Right> {
    rule("TVDeclO") {
        premise {
            ensure(Ts[id] == null)
            program.classByPath(jt)!!
        }
        conclusion {
            left { !f && program.classByPath(jt) != null }
            right { VarDecl.Right(Tf, Ts + (Id(id) to tt(it, Und)), Tbf, Tbs + (Id(id) to BottomTC), Tret) }
        }
    }

    rule("TVDeclB") {
        premise {}
        conclusion {
            left { !f && program.classByPath(jt) == null }
            right { TODO() }
        }
    }

    rule("TVDeclF") {
        premise {  }
        conclusion {
            left { true }
            right { TODO() }
        }
    }
}