package processor

import rules.CLASS_JUDGMENT
import rules.Clss
import rules.dsl.JudgementResult
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

class TypestateProcessor: Processor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (c in roundEnv.rootElements.filter { it.kind == ElementKind.CLASS }) {
            val derivation = CLASS_JUDGMENT(Clss.Left(program.classByElement(c), program))
            when (derivation) {
                is JudgementResult.Derived -> continue
                is JudgementResult.Ambiguous -> emit("ambiguous", c)
                is JudgementResult.NotDerivable -> emit("not derivable", c)
            }
        }
        return true
    }
}