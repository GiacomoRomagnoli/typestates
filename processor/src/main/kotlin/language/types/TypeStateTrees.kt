package language.types

import language.model.BottomClass
import language.model.ClassRef
import language.model.JavaClass
import language.model.JavaMethod
import language.model.at
import language.model.isSubClassOf

data class TypeStateTree(val classType: ClassType, val children: List<TypeStateTree> = emptyList()) : TC {
    constructor(clazz: ClassRef, type: T, children: List<TypeStateTree> = emptyList())
            : this(clazz at type, children)

    val clazz get() =  classType.clazz
    val type get() = classType.type
    val isWellFormed: Boolean by lazy {
        classType.isWellFormed &&
        nodup(this) &&
        when(clazz) {
            is BottomClass -> children.isEmpty()
            is JavaClass -> children.all { child ->
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
}

fun tt(c: ClassRef, t: T, vararg children: TypeStateTree) = TypeStateTree(c, t, children.toList())
fun tt(c: ClassRef, t: T, children: List<TypeStateTree>) = TypeStateTree(c, t, children)

fun ucastTT(tt: TypeStateTree, target: JavaClass): TypeStateTree {
    require(tt.clazz isSubClassOf target)
    fun rec(tt: TypeStateTree, target: JavaClass): TypeStateTree {
        if (target == tt.clazz) return tt
        val superclass = tt.clazz.superclass ?: return tt
        val head =
            if (superclass.isLinear)
                TypeStateTree(
                    superclass,
                    ucast(tt.type, tt.clazz, superclass),
                    listOf(tt)
                )
            else if (tt.type.isTerminated)
                TypeStateTree(superclass, Shared, listOf(tt))
            else
                TypeStateTree(superclass, Top, listOf(tt))
        return rec(head, target)
    }
    return rec(tt, target)
}

fun dcastTT(tt: TypeStateTree, target: ClassRef): TypeStateTree {
    require(target isSubClassOf tt.clazz)
    val closest = closestTT(tt, target)
    return if (target == closest.clazz) closest
    else TypeStateTree(target, dcast(closest.type, closest.clazz, target))
}

fun closestTT(tt: TypeStateTree, c: ClassRef): TypeStateTree {
    require(c isSubClassOf tt.clazz)
    return when (val subTree = tt.children.find { c isSubClassOf it.clazz }) {
        null -> tt
        else -> closestTT(subTree, c)
    }
}

fun evoTTI(tt: TypeStateTree, mt: JavaMethod): TypeStateTree =
    TypeStateTree(
        tt.clazz,
        evoI(tt.type, mt),
        tt.children.map { child -> evoTTI(child, mt) }
    )

fun evoTTO(tt: TypeStateTree, l: String): TypeStateTree =
    TypeStateTree(
        tt.clazz,
        evoO(tt.type, l),
        tt.children.map { child -> evoTTO(child, l) }
    )

fun resolveTT(tt: TypeStateTree): TypeStateTree =
    TypeStateTree(
        tt.clazz,
        resolve(tt.type),
        tt.children.map { child -> resolveTT(child) }
    )

fun mergeTT(tt1: TypeStateTree, tt2: TypeStateTree): TypeStateTree {
    require(tt1.clazz == tt2.clazz)
    fun rec(tree1: TypeStateTree, tree2: TypeStateTree): TypeStateTree =
        TypeStateTree(
            tree1.clazz,
            tree1.type and tree2.type,
            clss(tree1).intersect(clss(tree2))
                .map { rec(find(tree1, it)!!, find(tree2, it)!!) },
        )
    return rec(tt1, tt2)
}

infix fun TypeStateTree.sub(other: TypeStateTree): Boolean =
    if (this.isWellFormed && other.isWellFormed) {
        if (this.clazz != other.clazz && this.clazz isSubClassOf other.clazz) {
            this sub dcastTT(other, this.clazz)
        } else {
            val intersection = clss(this).intersect(clss(other))
            val subtraction = clss(other) - intersection
            this.classType sub other.classType &&
            intersection.all { find(this, it)!! sub find(other, it)!!} &&
            subtraction.all { dcastTT(this, it) sub find(other, it)!! }
        }
    } else false

fun invertTT(tt: TypeStateTree): TypeStateTree =
    tt(tt.clazz, invert(tt.type), tt.children.map { invertTT(it) })

fun clss(tt: TypeStateTree) =
    tt.children.map { it.clazz }.toSet()

fun find(tt: TypeStateTree, c: ClassRef) =
    tt.children.find { it.clazz == c }

fun nodup(tt: TypeStateTree) =
    tt.children.map { it.clazz }.toSet().size == tt.children.size