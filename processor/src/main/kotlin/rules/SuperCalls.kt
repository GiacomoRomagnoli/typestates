package rules

import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.util.TreePath
import language.model.JavaClass
import language.model.Program
import language.types.Delta
import language.types.THIS
import language.types.TypeEnv
import language.types.TypeStateTree
import language.types.sub
import language.types.toTC
import rules.dsl.Judgement
import rules.dsl.judgement

object SuperCall {
    data class Left(val Ts: TypeEnv, val program: Program, val path: TreePath) {
        val call = path.leaf
    }
}

val SUPER_CALL_JUDGEMENT: Judgement<SuperCall.Left, Delta> = judgement {
    rule("TSup1") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            ensure(c.superclass != null)
            ensure(c.superclass?.qualifiedName != "java.lang.Object")
            val superCall = call as MethodInvocationTree
            val args = superCall.arguments.map { TreePath(path, it) }
            val argsJdg = EXPRESSION_SEQUENCE_JUDGMENT.derive(
                ExprSeq.Left(emptyMap(), Ts, true, program, args)
            )
            ensure(argsJdg.Tf.isEmpty())
            val constructor = program.constructorByPath(path) ?: fail()
            ensure(argsJdg.tcs.size == constructor.pt.size)
            ensure(argsJdg.tcs.indices.all { i -> argsJdg.tcs[i] sub toTC(constructor.pt[i].type) })
            CONSTRUCTOR_JUDGMENT.derive(Cns(constructor, program)) to argsJdg.Ts
        }
        conclusion {
            left { ((call as? MethodInvocationTree)?.methodSelect as? IdentifierTree)?.name?.toString() == "super" }
            right { it }
        }
    }
}