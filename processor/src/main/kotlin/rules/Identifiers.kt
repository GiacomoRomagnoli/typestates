package rules

import com.sun.source.tree.ExpressionTree
import language.model.JavaClass
import language.types.Delta
import language.types.Eid
import language.types.TC
import language.types.THIS
import language.types.TypeStateTree
import language.types.Und
import language.types.alias
import language.types.get
import language.types.lookup
import language.types.sub
import language.types.upd
import rules.dsl.Judgement
import rules.dsl.judgement
import rules.utils.toEid

private data class TIdO1(val tc: TC, val delta: Delta)
private data class TIdO2(val c: JavaClass, val eid: Eid, val tc: TC, val delta: Delta)

val IDENTIFIER_JUDGEMENT: Judgement<Expr.Left, Expr.Right> = judgement {

    rule<TIdO1>("TIdO1") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val eid = (expression as? ExpressionTree)?.toEid() ?: fail()
            val tt = (lookup(c, eid, Tf to Ts) as? TypeStateTree) ?: fail()
            ensure(tt.isWellFormed && !(Und sub tt.type))
            TIdO1(tt, Tf to Ts)
        }
        conclusion {
            left { !a }
            right { Expr.Right(it.tc, it.delta) }
        }
    }

    rule<TIdO2>("TIdO2") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val eid = (expression as? ExpressionTree)?.toEid() ?: fail()
            val tt = (lookup(c, eid, Tf to Ts) as? TypeStateTree) ?: fail()
            ensure(tt.isWellFormed && !(Und sub tt.type))
            TIdO2(c, eid, tt, Tf to Ts)
        }
        conclusion {
            left { a }
            right { Expr.Right(it.tc, upd(it.c, it.eid, alias(it.tc), it.delta)) }
        }
    }
}