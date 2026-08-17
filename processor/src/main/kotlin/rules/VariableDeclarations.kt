package rules

import com.sun.source.tree.PrimitiveTypeTree
import com.sun.source.util.TreePath
import language.model.Program
import language.types.BoolUnd
import language.types.BottomTC
import language.types.DoubleUnd
import language.types.EnumType
import language.types.Id
import language.types.IntegerUnd
import language.types.get
import language.types.TypeEnv
import language.types.Und
import language.types.tt
import rules.dsl.judgement
import javax.lang.model.type.TypeKind

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
        premise {
            ensure(Ts[id] == null)
            when (val t = jt.leaf) {
                is PrimitiveTypeTree -> when (t.primitiveTypeKind) {
                    TypeKind.BOOLEAN -> BoolUnd
                    TypeKind.INT -> IntegerUnd
                    TypeKind.DOUBLE -> DoubleUnd
                    else -> fail()
                }
                else -> program.enumByTypePath(jt)?.let { EnumType(it, true) } ?: fail()
            }
        }
        conclusion {
            left { !f && program.classByPath(jt) == null }
            right { VarDecl.Right(Tf, Ts + (Id(id) to it), Tbf, Tbs + (Id(id) to BottomTC), Tret) }
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