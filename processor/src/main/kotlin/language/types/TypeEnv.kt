package language.types

import language.model.JavaClass

sealed interface Cid { val id: String }
data class FieldId(val c: JavaClass, override val id: String) : Cid
data class Id(override val id: String) : Cid
val THIS = Id("this")
typealias TypeEnv = Map<Cid, TC>
typealias Delta = Pair<TypeEnv, TypeEnv>

operator fun JavaClass.plus(id: String) = FieldId(this, id)
operator fun TypeEnv.get(id: String) = this[Id(id)]
fun TypeEnv.bottom(): TypeEnv = mapValues { BottomTC }
val Delta.Tf get() = this.first
val Delta.Ts get() = this.second

fun term(typeEnv: TypeEnv) = typeEnv.all { (_, tc) -> term(tc) }

fun resolve(typeEnv: TypeEnv): TypeEnv =
    typeEnv.mapValues { (_, tc) -> if (tc is TypeStateTree) resolveTT(tc) else tc}.toMap()

fun evolve(typeEnv: TypeEnv, l: String): TypeEnv =
    typeEnv.mapValues { (_, tc) -> if (tc is TypeStateTree) evoTTO(tc, l) else tc }.toMap()

fun merge(typeEnv1: TypeEnv, typeEnv2: TypeEnv): TypeEnv =
    require(typeEnv1.size == typeEnv2.size && typeEnv1.keys.containsAll(typeEnv2.keys))
        .let { typeEnv1.mapValues { (cid, tc) -> mergeTC(tc, typeEnv2[cid]!!) } }

fun List<TypeEnv>.merge() = reduce { env1, env2 -> merge(env1, env2) }

fun restrict(typeEnv: TypeEnv, clazz: JavaClass): TypeEnv =
    typeEnv.filter { (cid, _) -> cid is FieldId && clazz isSubClassOf cid.c }

operator fun TypeEnv.not(): TypeEnv =
    this.mapValues { (_, tc) -> if (tc is TypeStateTree) invertTT(tc) else tc }

fun pairify(typeEnv: TypeEnv, l: String): TypeEnv =
    typeEnv.mapValues { (_, tc) -> if (tc is TypeStateTree) toPairTT(tc, l) else tc }

data class Eid(val id: String, val receiver: Receiver)
enum class Receiver { THIS, SUPER, NONE }

fun lookup(c: JavaClass, eid: Eid, delta: Delta): TC? {
    val cAllF = c.allF(eid.id)
    val supcAllF = c.superclass?.allF(eid.id)
    return when {
        eid.receiver == Receiver.THIS && cAllF != null -> delta.Tf[cAllF + eid.id]
        eid.receiver == Receiver.SUPER && supcAllF != null -> delta.Tf[supcAllF + eid.id]
        eid.receiver == Receiver.NONE && cAllF != null && delta.Ts[eid.id] == null -> delta.Tf[cAllF + eid.id]
        eid.receiver == Receiver.NONE && delta.Ts[eid.id] != null -> delta.Ts[eid.id]
        else -> BottomTC
    }
}

fun upd(c: JavaClass, eid: Eid, tc: TC, delta: Delta) : Delta {
    val cAllF = c.allF(eid.id)
    val supcAllF = c.superclass?.allF(eid.id)
    return when {
        eid.receiver == Receiver.THIS && cAllF != null ->
            delta.Tf + (cAllF + eid.id to tc) to delta.Ts
        eid.receiver == Receiver.SUPER && supcAllF != null ->
            delta.Tf + (supcAllF + eid.id to tc) to delta.Ts
        eid.receiver == Receiver.NONE && cAllF != null && delta.Ts[eid.id] == null ->
            delta.Tf + (cAllF + eid.id to tc) to delta.Ts
        eid.receiver == Receiver.NONE && delta.Ts[eid.id] != null ->
            delta.Tf to delta.Ts + (Id(eid.id) to tc)
        else -> delta
    }
}