package processor

import rules.CLASS_JUDGMENT
import rules.Clss
import rules.dsl.JudgementResult
import rules.dsl.formatTrace
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

class TypestateProcessor: Processor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (c in roundEnv.rootElements.filter { it.kind == ElementKind.CLASS }) {
            val result = CLASS_JUDGMENT(Clss.Left(program.classByElement(c), program))
            if (result is JudgementResult.Derived)
                continue
            emit(result.formatTrace(), c)
        }
        return true
    }
}