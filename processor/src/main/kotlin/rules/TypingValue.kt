package rules

import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.ImportTree
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
import rules.dsl.Judgement

data class Value(
    val value: ExpressionTree,
    val program: Program,
    private val imports: List<ImportTree>,
    private val packageName: String? = null,
) {
    val MemberSelectTree.name get() = run {
        val name = this.toString()
        if ('.' in name) name
        else imports
            .filterNot { it.isStatic }
            .map { it.qualifiedIdentifier.toString() }
            .firstOrNull { it.substringAfterLast(".") == name }
            ?: packageName?.let { "$it.$name" }
            ?: name

    }
}

val typingValue = Judgement<Value, TC>()

val TInt = typingValue.rule("TInt") {
    premise {  }
    conclusion { left { value.kind == INT_LITERAL  } ; right { Integer } }
}

val TDouble = typingValue.rule("TDouble") {
    premise {  }
    conclusion { left { value.kind == DOUBLE_LITERAL } ; right { Double } }
}

val TBool = typingValue.rule("TBool") {
    premise {  }
    conclusion { left { value.kind == BOOLEAN_LITERAL } ; right { Bool } }
}

val TEnumVal = typingValue.rule("TEnumVal") {
    premise {
        val value = value as MemberSelectTree
        val enum = program.enumOf(value.name)
        ensure(enum != null)
        ensure(value.identifier.toString() in enum!!.labels)
        enum
    }
    conclusion { left { value is MemberSelectTree } ; right { EnumType(it) } }
}

val TNull = typingValue.rule("TNull") {
    premise {  }
    conclusion { left { value.kind ==  NULL_LITERAL} ; right { tt(BottomClass, Null) }}
}