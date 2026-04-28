package ast

import org.antlr.v4.runtime.Token
import java.nio.file.Paths

data class Position(
    val filename: String,
    val startOffset: Int,
    val endOffset: Int,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int
) {
    val basename: String
        get() = Paths.get(filename).fileName.toString()

    val startLineCol: String
        get() = "$startLine:$startColumn"

    val endLineCol: String
        get() = "$endLine:$endColumn"

    companion object {
        @JvmStatic
        fun fromToken(token: Token): Position {
            val textLength = token.text?.length ?: 0
            return Position(
                filename = token.tokenSource.sourceName,
                startOffset = token.startIndex,
                endOffset = token.startIndex + textLength,
                startLine = token.line,
                startColumn = token.charPositionInLine,
                endLine = token.line,
                endColumn = token.charPositionInLine + textLength
            )
        }

        @JvmStatic
        fun fromTokens(start: Token, end: Token): Position {
            val endLength = end.text?.length ?: 0
            return Position(
                filename = start.tokenSource.sourceName,
                startOffset = start.startIndex,
                endOffset = end.startIndex + endLength,
                startLine = start.line,
                startColumn = start.charPositionInLine,
                endLine = end.line,
                endColumn = end.charPositionInLine + endLength
            )
        }
    }
}