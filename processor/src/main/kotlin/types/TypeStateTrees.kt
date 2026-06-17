package types

import classes.Class
import classes.LinearClass
import classes.NonLinearClass
import classes.sup
import processor.environment.Java

data class TypeStateTree(val c: Class, val t: Type, val children: List<TypeStateTree> = emptyList())

fun nodup(tree: TypeStateTree) = tree.children.map { it.c }.toSet().size == tree.children.size
val TypeStateTree.isWellFormed : Boolean
    get() {
        if (c is LinearClass && !typestates(t).all { it in c.protocol.protSt }) return false
        else if (c is NonLinearClass && typestates(t).isNotEmpty()) return false
        if (!nodup(this)) return false
        for (child in children) {
            if (sup(child.c) != c) return false
            if (!child.isWellFormed) return false
            if (child.t.isResolved)
                if(!(child.c typed child.t sub (c typed t))) return false
            else if (!child.t.labels.all { l -> child.c typed evoO(child.t, l) sub (c typed evoO(t, l)) }) return false
        }
        return true
    }
fun ucastTT(tree: TypeStateTree, target: Class): TypeStateTree? {
    if (!Java.types.isSubtype(tree.c.element.asType(), target.element.asType())) return null
    if (target == tree.c) return tree
    val superC = sup(tree.c)
    return when(tree.c) {
        is LinearClass -> when(superC) {
            is NonLinearClass -> ucastTT(TypeStateTree(superC, Top, listOf(tree)), target)
            is LinearClass -> ucastTT(TypeStateTree(superC, ucast(tree.t, tree.c, superC), listOf(tree)), target)
        }
        is NonLinearClass -> when(superC) {
            is NonLinearClass -> ucastTT(TypeStateTree(superC, Top, listOf(tree)), target)
            is LinearClass -> TODO()
        }
    }
}
fun closestTT(tree: TypeStateTree, c: Class): TypeStateTree? {
    if (!Java.types.isSubtype(c.element.asType(), tree.c.element.asType())) return null
    fun recursion(tree: TypeStateTree, c: Class): TypeStateTree {
        val subTree = tree.children.find { Java.types.isSubtype(c.element.asType(), it.c.element.asType()) }
        if (subTree != null) return recursion(subTree, c)
        return tree
    }
    return recursion(tree, c)
}