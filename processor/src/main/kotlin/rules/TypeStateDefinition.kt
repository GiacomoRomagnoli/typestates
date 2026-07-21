package rules

import language.model.JavaClass
import protocol.model.State
import protocol.model.TypeState
import rules.dsl.Judgement

data class TypeStateDefinitionInput(
    val clazz: JavaClass,
    val state: State,
    val env: TypeEnv
)

val typestateDefinition = Judgement<TypeStateDefinitionInput, TypeEnv>()