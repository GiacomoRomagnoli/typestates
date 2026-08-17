package rules.utils

import com.sun.source.tree.ExpressionStatementTree
import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.tree.StatementTree
import com.sun.source.tree.Tree
import com.sun.source.util.TreePath
import language.types.Eid
import language.types.Receiver
import rules.Expr
import javax.lang.model.element.Name

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

fun Expr.Left.isLabel(path: TreePath) =
    path.leaf.kind == Tree.Kind.BOOLEAN_LITERAL || program.enumByValuePath(path) != null

fun Name.toEid() = Eid(toString(), Receiver.NONE)

fun StatementTree.isSuperCall(): Boolean {
    val expr = (this as? ExpressionStatementTree)
        ?.expression as? MethodInvocationTree
        ?: return false
    val select = expr.methodSelect as? IdentifierTree
        ?: return false
    return select.name.toString() == "super"
}