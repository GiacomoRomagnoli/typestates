package language.types

import language.model.JavaClass
import language.model.JavaMethod

data class TypeStateTree(val classType: ClassType, val children: List<TypeStateTree> = emptyList()) {
    constructor(clazz: JavaClass, type: T, children: List<TypeStateTree> = emptyList())
            : this(clazz at type, children)

    val clazz = classType.clazz
    val type = classType.type
    private val nodup = children.map { it.clazz }.toSet().size == children.size
    val isWellFormed: Boolean by lazy {
        if (clazz.isLinear)
            typestates(type).all { it in clazz.protocol!!.protSt }
        else
            typestates(type).isEmpty()
        &&
        nodup &&
        children.all { child ->
            child.clazz.superclass == clazz &&
            child.isWellFormed &&
            if (child.type.isResolved)
                child.classType sub classType
            else
                child.type.labels.all { l ->
                    (child.clazz at evoO(child.type, l)) sub (clazz at evoO(type, l))
                }
        }
    }
}

fun ucastTT(tree: TypeStateTree, target: JavaClass): TypeStateTree {
    require(tree.clazz isSubClassOf target)
    fun rec(tree: TypeStateTree, target: JavaClass): TypeStateTree {
        if (target == tree.clazz) return tree
        val superclass = tree.clazz.superclass ?: return tree
        val head =
            if (superclass.isLinear) TypeStateTree(superclass, ucast(tree.type, tree.clazz, superclass), listOf(tree))
            else TypeStateTree(superclass, Top, listOf(tree))
        return ucastTT(head, target)
    }
    return rec(tree, target)
}

fun dcastTT(tree: TypeStateTree, target: JavaClass): TypeStateTree {
    require(target isSubClassOf tree.clazz)
    val closest = closestTT(tree, target)
    return if (target == closest.clazz) closest
    else TypeStateTree(target, dcast(closest.type, closest.clazz, target))
}

fun closestTT(tree: TypeStateTree, c: JavaClass): TypeStateTree {
    require(c isSubClassOf tree.clazz)
    return when (val subTree = tree.children.find { c isSubClassOf it.clazz }) {
        null -> tree
        else -> closestTT(subTree, c)
    }
}

fun evoTTI(tree: TypeStateTree, mt: JavaMethod): TypeStateTree =
    TypeStateTree(
        tree.clazz,
        evoI(tree.type, mt),
        tree.children.map { child -> evoTTI(child, mt) }
    )

fun evoTTO(tree: TypeStateTree, l: String): TypeStateTree =
    TypeStateTree(
        tree.clazz,
        evoO(tree.type, l),
        tree.children.map { child -> evoTTO(child, l) }
    )

fun resolveTT(tree: TypeStateTree): TypeStateTree =
    TypeStateTree(
        tree.clazz,
        resolve(tree.type),
        tree.children.map { child -> resolveTT(child) }
    )

fun mergeTT(tree1: TypeStateTree, tree2: TypeStateTree): TypeStateTree {
    require(tree1.clazz == tree2.clazz)
    fun rec(tree1: TypeStateTree, tree2: TypeStateTree): TypeStateTree =
        TypeStateTree(
            tree1.clazz,
            tree1.type and tree2.type,
            tree1.children.mapNotNull { child ->
                tree2.children.find { child.clazz == it.clazz }?.let { mergeTT(child, it) }
            }
        )
    return rec(tree1, tree2)
}

infix fun TypeStateTree.sub(other: TypeStateTree): Boolean {
    if (!this.isWellFormed || !other.isWellFormed) return false
    fun recursion(tree1: TypeStateTree, tree2: TypeStateTree): Boolean {
        if (!(this.classType sub other.classType)) return false
        if (!tree1.children.mapNotNull { child -> tree2.children.find { child.clazz == it.clazz }?.let { child to it } }.all { (t1, t2) -> t1 sub t2 }) return false
        if (!tree2.children.filter { child -> tree1.children.find { it.clazz == child.clazz } == null }.all { dcastTT(tree1, it.clazz) sub it}) return false
        return true
    }
    return if (this.clazz == other.clazz) recursion(this, other) else recursion(this, dcastTT(other, this.clazz))
}