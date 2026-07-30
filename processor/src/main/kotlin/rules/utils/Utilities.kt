package rules.utils

import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.MemberSelectTree
import language.types.Eid
import language.types.Receiver

fun ExpressionTree.toEid(): Eid? =
    when (this) {
        is IdentifierTree ->
            name.toString()
                .takeUnless { it == "this" || it == "super" }
                ?.let { Eid(it, Receiver.NONE) }

        is MemberSelectTree ->
            when ((expression as? IdentifierTree)?.name?.toString()) {
                "this" -> Eid(identifier.toString(), Receiver.THIS)
                "super" -> Eid(identifier.toString(), Receiver.SUPER)
                else -> null
            }

        else -> null
    }