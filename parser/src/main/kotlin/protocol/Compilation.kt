package protocol

import ast.JavaTypeNode
import ast.MethodNode
import ast.OutPutStateNode
import ast.ProtocolNode
import ast.TypeStateNode
import ast.TypeStateRefNode
import protocol.error.SemanticError
import protocol.model.JavaType
import protocol.model.Method
import protocol.model.OutPutState
import protocol.model.Protocol
import protocol.model.TypeState
import kotlin.collections.set

fun compile(ast: ProtocolNode): ProtocolCompilation {
    val errors = ast.analyse()
    val protocolBinding = ProtocolBinding()
    val protocol = Protocol(ast.name.value)
    val typeStates = ast.states.map { build(it, protocol, protocolBinding) }
    protocol.initState = typeStates.first()
    typeStates.forEach { protocol.typeStates.putIfAbsent(it.name, it) }
    protocol.typeStates["end"] =
        TypeState(
            "end",
            false,
            protocol,
            emptyMap(),
            emptyMap()
        )
    return ProtocolCompilation(errors, protocol, protocolBinding)
}

private fun build(node: TypeStateNode, protocol: Protocol, protocolBinding: ProtocolBinding) =
    TypeState(
        node.name.value,
        node.droppable,
        protocol,
        node.transitions
            .filter { it.target is TypeStateRefNode }
            .associate { build(it.method, protocolBinding) to (it.target as TypeStateRefNode).name.value },
        node.transitions
            .filter { it.target is OutPutStateNode }
            .associate { build(it.method, protocolBinding) to build(it.target as OutPutStateNode, protocol, protocolBinding) }
    ).also { protocolBinding.associate(it, node) }

private fun build(node: OutPutStateNode, protocol: Protocol, protocolBinding: ProtocolBinding) =
    OutPutState(protocol, node.branches.associate { it.label.value to it.ref.name.value })
        .also { protocolBinding.associate(it, node) }

private fun build(node: MethodNode, protocolBinding: ProtocolBinding) =
    Method(node.name.value, node.args.map { build(it, protocolBinding) })
        .also { protocolBinding.associate(it, node) }

private fun build(node: JavaTypeNode, protocolBinding: ProtocolBinding) =
    JavaType(node.name.joinToString(".") { it.value }, node.arrayLevel)
        .also { protocolBinding.associate(it, node) }

internal fun ProtocolNode.analyse(): List<SemanticError> {
    val errors = mutableListOf<SemanticError>()
    val ids = states.map { it.name }
    for ((i, id) in ids.withIndex()) {
        for (j in i + 1 until ids.size) {
            if (id.value == ids[j].value) {
                errors.add(SemanticError("${id.value} already declared at ${id.position.startLineCol}"))
            }
        }
    }
    states.forEach {
        val methods = it.transitions.map { it.method }
        for ((i, m) in methods.withIndex()) {
            for (j in i + 1 until methods.size) {
                if (m.name.value == methods[j].name.value && m.args.size == methods[j].args.size) {
                    if (
                        m.args
                            .zip(methods[j].args)
                            .all { (arg1, arg2) ->
                                arg1.name.joinToString(".") { it.value} == arg2.name.joinToString(".") { it.value} &&
                                        arg1.arrayLevel == arg2.arrayLevel
                            }
                    ) {
                        errors.add(
                            SemanticError(
                                "the use of ${methods[j].name.value} at ${methods[j].position.startLineCol}" +
                                        "makes transition non deterministic"
                            )
                        )
                    }
                }
            }
        }
    }
    val refs = states.flatMap { it.transitions }.map { it.target }.flatMap {
        when (it) {
            is TypeStateRefNode -> listOf(it)
            is OutPutStateNode -> it.branches
                .map { branch -> branch.ref }
        }
    }
    for (ref in refs) {
        if(ref.name.value != "end" && states.find { it.name.value == ref.name.value } == null) {
            errors.add(SemanticError("${ref.name.value} at ${ref.position.startLineCol} not declared"))
        }
    }
    return errors
}
