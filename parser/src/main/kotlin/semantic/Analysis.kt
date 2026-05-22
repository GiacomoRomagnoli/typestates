package semantic

import ast.OutPutStateNode
import ast.TypeStateRefNode
import ast.ProtocolNode
import semantic.error.SemanticError
import kotlin.collections.flatMap

fun ProtocolNode.analyse(): List<SemanticError> {
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