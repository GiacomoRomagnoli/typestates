package semantic

import TypestateLexer
import TypestateParser
import ast.BranchNode
import ast.DecisionTargetNode
import ast.EndStateNode
import ast.StateRefNode
import ast.TypeStateNode
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

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
                is DecisionTargetNode ->
                    it.branches.map { branch -> branch.target }.filterIsInstance<StateRefNode>().map { it.name }
            }
        }
        for (ref in refs) {
            if(ids.none { id -> id.value == ref.value }) {
                throw SemanticException("${ref.value} at ${ref.position.startLineCol} not declared")
            }
        }
        return this
    }
}