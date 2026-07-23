package language.types

import language.model.JavaClass

sealed class Cid { abstract val id: String }
data class FieldId(val c: JavaClass, override val id: String) : Cid()
data class Id(override val id: String) : Cid()
typealias TypeEnv = Map<Cid, TC>

fun term(typeEnv: TypeEnv) = typeEnv.all { (_, tc) -> term(tc) }

fun resolve(typeEnv: TypeEnv): TypeEnv =
    typeEnv.mapValues { (_, tc) -> if (tc is TypeStateTree) resolveTT(tc) else tc}.toMap()

fun evolve(typeEnv: TypeEnv, l: String): TypeEnv =
    typeEnv.mapValues { (_, tc) -> if (tc is TypeStateTree) evoTTO(tc, l) else tc }.toMap()

fun merge(typeEnv1: TypeEnv, typeEnv2: TypeEnv): TypeEnv =
    require(typeEnv1.size == typeEnv2.size && typeEnv1.keys.containsAll(typeEnv2.keys))
        .let { typeEnv1.mapValues { (cid, tc) -> mergeTC(tc, typeEnv2[cid]!!) } }

fun restrict(typeEnv: TypeEnv, clazz: JavaClass): TypeEnv =
    typeEnv.filter { (cid, _) -> cid is FieldId && clazz isSubClassOf cid.c }

operator fun TypeEnv.not() =
    this.mapValues { (_, tc) -> if (tc is TypeStateTree) invertTT(tc) else tc }