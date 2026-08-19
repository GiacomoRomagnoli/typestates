package rules

import com.sun.source.tree.ExpressionTree
import language.model.JavaClass
import language.types.BoolUnd
import language.types.DoubleUnd
import language.types.Eid
import language.types.EnumType
import language.types.IntegerUnd
import language.types.TC
import language.types.THIS
import language.types.TypeStateTree
import language.types.Und
import language.types.alias
import language.types.lookup
import language.types.sub
import language.types.upd
import rules.dsl.Judgement
import rules.dsl.judgement
import rules.utils.toEid

private data class TId(val c: JavaClass, val eid: Eid, val tc: TC)

val IDENTIFIER_JUDGEMENT: Judgement<Expr.Left, Expr.Right> = judgement {

    rule("TIdO1") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val eid = (expression as? ExpressionTree)?.toEid() ?: fail()
            val tt = (lookup(c, eid, Tf to Ts) as? TypeStateTree) ?: fail()
            ensure(tt.isWellFormed && !(Und sub tt.type))
            tt
        }
        conclusion {
            left { !a }
            right { Expr.Right(it, Tf, Ts) }
        }
    }

    rule<TId>("TIdO2") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val eid = (expression as? ExpressionTree)?.toEid() ?: fail()
            val tt = (lookup(c, eid, Tf to Ts) as? TypeStateTree) ?: fail()
            ensure(tt.isWellFormed && !(Und sub tt.type))
            TId(c, eid, tt)
        }
        conclusion {
            left { a }
            right { Expr.Right(it.tc, upd(it.c, it.eid, alias(it.tc), Tf to Ts)) }
        }
    }

    rule("TIdB1") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val eid = (expression as? ExpressionTree)?.toEid() ?: fail()
            val tc = lookup(c, eid, Tf to Ts) ?: fail()
            ensure(tc !is TypeStateTree)
            ensure(tc !in listOf(IntegerUnd, DoubleUnd, BoolUnd))
            ensure(!(tc is EnumType && tc.und))
            tc
        }
        conclusion {
            left { !a }
            right { Expr.Right(it, Tf, Ts) }
        }
    }

    rule<TId>("TIdB2") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val eid = (expression as? ExpressionTree)?.toEid() ?: fail()
            val tc = lookup(c, eid, Tf to Ts) ?: fail()
            ensure(tc !is TypeStateTree)
            ensure(tc !in listOf(IntegerUnd, DoubleUnd, BoolUnd))
            ensure(!(tc is EnumType && tc.und))
            TId(c, eid, tc)
        }
        conclusion {
            left { a }
            right { Expr.Right(it.tc, upd(it.c, it.eid, alias(it.tc), Tf to Ts)) }
        }
    }
}