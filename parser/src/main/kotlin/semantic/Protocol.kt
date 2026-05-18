package semantic

import TypestateLexer
import TypestateParser
import ast.OutPutStateNode
import ast.TypeStateNode
import ast.TypeStateRefNode
import ast.ProtocolNode
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.collections.flatMap

object Protocol {
    fun parse(input: String): ProtocolNode =
        TypestateParser(CommonTokenStream(TypestateLexer(CharStreams.fromString(input)))).typestate().node

    fun ProtocolNode.validate(): ProtocolNode {
        val ids = this.states.map { it.name }
        for ((i, id) in ids.withIndex()) {
            for (j in i + 1 until ids.size) {
                if (id.value == ids[j].value) {
                    throw SemanticException("${id.value} already declared at ${id.position.startLineCol}")
                }
            }
        }
        val refs = this.states.flatMap { it.transitions }.map { it.target }.flatMap {
            when (it) {
                is TypeStateRefNode -> listOf(it)
                is OutPutStateNode -> it.branches
                    .map { branch -> branch.target }
            }
        }
        for (ref in refs) {
            if(!ref.isEnd() && resolve(ref) == null) {
                throw SemanticException("${ref.name.value} at ${ref.position.startLineCol} not declared")
            }
        }
        return this
    }

    fun ProtocolNode.resolve(ref: TypeStateRefNode): TypeStateNode? =
        states.find { it.name.value == ref.name.value }

    fun ProtocolNode.next(state: TypeStateNode): Set<TypeStateNode> =
        state.transitions
            .map { it.target }
            .flatMap {
                when (it) {
                    is TypeStateRefNode -> listOf(it)
                    is OutPutStateNode -> it.branches.map { branch -> branch.target }
                }
            }
            .mapNotNull { resolve(it) }
            .toSet()

    fun ProtocolNode.protIn(): Set<TypeStateNode> {
        fun reach(reachable: Set<TypeStateNode>, reached: Set<TypeStateNode>): Set<TypeStateNode> {
            val discovered = reachable.flatMap { next(it) }.toSet()
            return if (discovered.isEmpty()) {
                reached
            } else {
                reach(discovered - reached, reached + reachable)
            }
        }
        return reach(setOf(states.first()), setOf())
    }

    fun OutPutStateNode.labels(): Set<String> = branches.map { it.label.value }.toSet()
    fun TypeStateRefNode.isEnd(): Boolean = name.value == "end"
}