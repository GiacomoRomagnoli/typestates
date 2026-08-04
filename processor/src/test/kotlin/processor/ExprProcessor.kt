package processor

import com.sun.source.tree.BlockTree
import com.sun.source.util.TreePath
import language.model.JavaClass
import language.model.JavaMethod
import language.types.Id
import language.types.Shared
import language.types.TypeEnv
import language.types.U
import language.types.toTC
import language.types.tt
import rules.STATEMENT_SEQUENCE_JUDGMENT
import rules.StmtSeq
import rules.dsl.JudgementResult
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class ExprProcessor: Processor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (element in roundEnv.rootElements.filter { it.kind == ElementKind.CLASS }) {
            val c = program.classByElement(element)
            for (mt in c.meths) {
                val body = mt.body ?: continue
                val statements = (body.leaf as BlockTree).statements.map { TreePath(body, it) }
                val input = StmtSeq.Left(
                    fields = emptyMap(),
                    variables = variables(c, mt),
                    statements = statements,
                    program = program
                )
                when (val result = STATEMENT_SEQUENCE_JUDGMENT(input)) {
                    is JudgementResult.Derived ->
                        processingEnv.messager.printMessage(
                            Diagnostic.Kind.NOTE,
                            result.value.variables.toString()
                        )
                    is JudgementResult.Ambiguous ->
                        emit("Ambiguous judgement: ${result.rules}", mt.element)
                    JudgementResult.NotDerivable ->
                        emit("Method body is not typable", mt.element)
                }
            }
        }
        return true
    }

    private fun variables(c: JavaClass, mt: JavaMethod): TypeEnv = buildMap {
        put(Id("this"), c.protocol?.let { tt(c, U(it.initState)) } ?: tt(c, Shared))
        mt.element.parameters
            .zip(mt.pt)
            .forEach { (parameter, parameterType) ->
                put(Id(parameter.simpleName.toString()), toTC(parameterType))
            }
    }
}