package rules

import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.Tree.Kind.*
import language.model.BottomClass
import language.model.Program
import language.types.Bool
import language.types.Double
import language.types.EnumType
import language.types.Integer
import language.types.Null
import language.types.TC
import language.types.tt
import rules.dsl.judgement

data class Value(
    private val locatedExpression: LocatedExpression,
    val program: Program
) {
    val value get() = locatedExpression.expr
    val path get() = locatedExpression.path
}

val typingValue = judgement<Value, TC> {

    rule("TInt") {
        premise {  }
        conclusion {
            left { value.kind == INT_LITERAL  }
            right { Integer }
        }
    }

    rule("TDouble") {
        premise {  }
        conclusion {
            left { value.kind == DOUBLE_LITERAL }
            right { Double }
        }
    }

    rule("TBool") {
        premise {  }
        conclusion {
            left { value.kind == BOOLEAN_LITERAL }
            right { Bool }
        }
    }

    rule("TEnumVal") {
        premise {
            val enum = program.asJavaEnum(path) ?: fail()
            val l = (value as MemberSelectTree).identifier.toString()
            ensure(l in enum.labels)
            enum
        }
        conclusion {
            left { value is MemberSelectTree }
            right { EnumType(it) }
        }
    }

    rule("TNull") {
        premise {  }
        conclusion {
            left { value.kind ==  NULL_LITERAL}
            right { tt(BottomClass, Null) }
        }
    }
}