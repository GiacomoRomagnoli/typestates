package semantic

import TypestateLexer
import TypestateParser
import ast.DecisionTargetNode
import ast.EndStateNode
import ast.IdNode
import ast.StateNode
import ast.StateRefNode
import ast.StateTargetNode
import ast.TypeStateNode
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.collections.flatMap

object Protocol {
    fun parse(input: String): TypeStateNode =
        TypestateParser(CommonTokenStream(TypestateLexer(CharStreams.fromString(input)))).typestate().node

    fun TypeStateNode.validate(): TypeStateNode {
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
                is EndStateNode -> listOf()
                is StateRefNode -> listOf(it.name)
                is DecisionTargetNode -> it.branches
                    .map { branch -> branch.target }
                    .filterIsInstance<StateRefNode>()
                    .map { ref -> ref.name }
            }
        }
        for (ref in refs) {
            if(ids.none { id -> id.value == ref.value }) {
                throw SemanticException("${ref.value} at ${ref.position.startLineCol} not declared")
            }
        }
        return this
    }

    // end state represented by a StateNode with no transitions
    fun TypeStateNode.resolve(ref: StateTargetNode): StateNode? =
        when (ref) {
            is EndStateNode -> StateNode(
                ref.position,
                IdNode(ref.position, "end"),
                emptyList(),
                false
            )
            is StateRefNode -> states.find { it.name.value == ref.name.value }
        }

    fun TypeStateNode.next(state: StateNode): Set<StateNode> =
        state.transitions
            .map { it.target }
            .flatMap {
                when (it) {
                    is StateTargetNode -> listOf(it)
                    is DecisionTargetNode -> it.branches.map { branch -> branch.target }
                }
            }
            .mapNotNull { resolve(it) }
            .toSet()


    fun TypeStateNode.protIn(): Set<StateNode> {
        fun reach(reachable: Set<StateNode>, reached: Set<StateNode>): Set<StateNode> {
            val discovered = reachable.flatMap { next(it) }.toSet()
            return if (discovered.isEmpty()) {
                reached
            } else {
                reach(discovered - reached, reached + reachable)
            }
        }
        return reach(setOf(states.first()), setOf())
    }

    fun DecisionTargetNode.labels(): Set<String> = branches.map { it.label.value }.toSet()
}