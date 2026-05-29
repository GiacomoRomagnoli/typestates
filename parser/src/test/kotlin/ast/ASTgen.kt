package ast

import TypestateLexer
import TypestateParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

fun main() {
    val input = """
        typestate A { 
            S = { init() : N, init2() : N }
            N = { m(int) : end }
        }
    """.trimIndent()

    val parser = TypestateParser(
        CommonTokenStream(
            TypestateLexer(CharStreams.fromString(input))
        )
    )

    val ast = parser.typestate().node
    println(ast.prettyPrint())
}

fun TNode.prettyPrint(indent: String = ""): String {
    val next = "$indent  "

    return when (this) {
        is IdNode ->
            "${indent}Id(${value})"

        is ProtocolNode ->
            buildString {
                appendLine("${indent}TypeState ${name.value}")
                states.forEach { appendLine(it.prettyPrint(next)) }
            }

        is TypeStateNode ->
            buildString {
                appendLine("${indent}State ${name.value} (droppable=$droppable)")
                transitions.forEach { appendLine(it.prettyPrint(next)) }
            }

        is TransitionNode ->
            buildString {
                appendLine("${indent}Transition")
                appendLine(method.prettyPrint(next))
                appendLine(target.prettyPrint(next))
            }

        is MethodNode ->
            buildString {
                append("${indent}Method ${name.value}(")
                append(args.joinToString(", ") { it.prettyPrint() })
                append(")")
            }

        is JavaTypeNode ->
            buildString {
                append(name.joinToString(".") { it.value })
                repeat(arrayLevel) { append("[]") }
            }

        is TypeStateRefNode ->
            "${indent}StateRef ${name.value}"

        is OutPutStateNode ->
            buildString {
                appendLine("${indent}Decision")
                branches.forEach { appendLine(it.prettyPrint(next)) }
            }

        is BranchNode ->
            "${indent}Branch ${label.value} -> ${ref.prettyPrint("")}"

        is TargetNode -> error("Unhandled TargetNode")
    }
}