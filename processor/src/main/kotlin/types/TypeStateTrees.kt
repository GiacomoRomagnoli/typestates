package types

import classes.Class
import classes.LinearClass
import classes.NonLinearClass
import classes.sup
import processor.environment.Java
import semantic.model.Method

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
fun ucastTT(tree: TypeStateTree, target: Class): TypeStateTree {
    require(Java.types.isSubtype(tree.c.element.asType(), target.element.asType()))
    fun recursion(tree: TypeStateTree, target: Class): TypeStateTree {
        if (target == tree.c) return tree
        val superC = sup(tree.c)
        return when {
            tree.c is LinearClass && superC is LinearClass ->
                ucastTT(TypeStateTree(superC, ucast(tree.t, tree.c, superC), listOf(tree)), target)
            else -> ucastTT(TypeStateTree(superC, Top, listOf(tree)), target)
        }
    }
    return recursion(tree, target)
}
fun dcastTT(tree: TypeStateTree, target: Class): TypeStateTree {
    require(Java.types.isSubtype(target.element.asType(), tree.c.element.asType()))
    fun recursion(tree: TypeStateTree, target: Class): TypeStateTree {
        val closest = closestTT(tree, target)
        return if (target == closest.c) closest
        else when {
            closest.c is LinearClass && target is LinearClass ->
                TypeStateTree(target, dcast(closest.t, closest.c, target))
            else -> TODO()
        }
    }
    return recursion(tree, target)
}
fun closestTT(tree: TypeStateTree, c: Class): TypeStateTree {
    require(!Java.types.isSubtype(c.element.asType(), tree.c.element.asType()))
    fun recursion(tree: TypeStateTree, c: Class): TypeStateTree {
        val subTree = tree.children.find { Java.types.isSubtype(c.element.asType(), it.c.element.asType()) }
        if (subTree != null) return recursion(subTree, c)
        return tree
    }
    return recursion(tree, c)
}
fun evoTTI(tree: TypeStateTree, mt: Method): TypeStateTree =
    TypeStateTree(
        tree.c,
        evoI(tree.t, mt),
        tree.children.map { child -> evoTTI(child, mt) }
    )
fun evoTTO(tree: TypeStateTree, l: String): TypeStateTree =
    TypeStateTree(
        tree.c,
        evoO(tree.t, l),
        tree.children.map { child -> evoTTO(child, l) }
    )
fun resolveTT(tree: TypeStateTree): TypeStateTree =
    TypeStateTree(
        tree.c,
        resolve(tree.t),
        tree.children.map { child -> resolveTT(child) }
    )
fun mergeTT(tree1: TypeStateTree, tree2: TypeStateTree): TypeStateTree {
    require(tree1.c == tree2.c)
    fun recursion(tree1: TypeStateTree, tree2: TypeStateTree): TypeStateTree =
        TypeStateTree(
            tree1.c,
            tree1.t and tree2.t,
            tree1.children.mapNotNull { child -> tree2.children.find { child.c == it.c }?.let { mergeTT(child, it) } }
        )
    return recursion(tree1, tree2)
}
infix fun TypeStateTree.sub(other: TypeStateTree): Boolean {
    if (!this.isWellFormed || !other.isWellFormed) return false
    if (!Java.types.isSubtype(this.c.element.asType(), other.c.element.asType())) return false
    fun recursion(tree1: TypeStateTree, tree2: TypeStateTree): Boolean {
        if (!(c typed t sub (other.c typed other.t))) return false
        if (!tree1.children.mapNotNull { child -> tree2.children.find { child.c == it.c }?.let { child to it } }.all { (t1, t2) -> t1 sub t2 }) return false
        if (!tree2.children.filter { child -> tree1.children.find { it.c == child.c } == null }.all { dcastTT(tree1, it.c) sub it}) return false
        return true
    }
    return if (this.c == other.c) recursion(this, other) else recursion(this, dcastTT(other, this.c))
}