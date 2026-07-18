package rules

import language.model.JavaClass
import protocol.model.State
import protocol.model.TypeState
import rules.dsl.Judgement
import rules.dsl.satisfied

data class TypeStateDefinitionInput(
    val clazz: JavaClass,
    val state: State,
    val env: TypeEnv
)

val typestateDefinition = Judgement<TypeStateDefinitionInput, TypeEnv>("typestateDefinition")

val TEnd = typestateDefinition.rule("TEnd") {
    match { state is TypeState && state.isEnd }
    premise { satisfied() }
    conclusion { resolve(env) }
}