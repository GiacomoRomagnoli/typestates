package rules

import language.model.JavaClass
import language.model.Program
import language.types.TypeEnv
import language.types.bottom
import language.types.merge
import language.types.resolve
import language.types.restrict
import language.types.sub
import protocol.model.State
import protocol.model.TypeState
import rules.dsl.Judgement
import rules.dsl.judgement

typealias Theta = Map<TypeState, TypeEnv>

object TypeStateDef {
    data class Left(
        val theta: Theta,
        val fields: TypeEnv,
        val clazz: JavaClass,
        val state: State,
        val program: Program,
        val unfolded: Boolean = false,
    )
}

val TYPESTATE_DEFINITION_JUDGMENT: Judgement<TypeStateDef.Left, TypeEnv> =
    judgement {

        rule("TEnd") {
            premise { }
            conclusion {
                left { state is TypeState && state.isEnd }
                right { resolve(fields) }
            }
        }

        rule("TVar") {
            premise {
                val s = state as TypeState
                val tf = theta[s]!!
                ensure(resolve(fields).all { (cid, tc) -> tc sub (tf[cid] ?: return@all false) })
            }
            conclusion {
                left { !unfolded && state is TypeState && theta.containsKey(state) }
                right { fields.bottom() }
            }
        }

        rule("TRec") {
            premise {
                val s = state as TypeState
                val nextTheta = theta + (s to resolve(fields))
                TYPESTATE_DEFINITION_JUDGMENT.derive(
                    TypeStateDef.Left(nextTheta, fields, clazz, s, program, true)
                )
            }
            conclusion {
                left { !unfolded && state is TypeState && !state.isEnd && !theta.containsKey(state) }
                right { it }
            }
        }

        rule<List<TypeEnv>>("TBr") {
            premise {
                val u = state as TypeState
                val fieldsResolved = resolve(fields)
                u.transitions.map {
                    val method = clazz.method(it.method) ?: fail()
                    val owner = clazz.allM(method) ?: fail()
                    val fieldsRestricted = restrict(fieldsResolved, owner)
                    val methodFields = METHOD_JUDGEMENT.derive(
                        Meth.Left(
                            fieldsRestricted,
                            method,
                            program,
                            owner
                        )
                    )
                    TYPESTATE_DEFINITION_JUDGMENT.derive(
                        TypeStateDef.Left(
                            theta,
                            (fieldsResolved - fieldsRestricted.keys) + methodFields,
                            clazz,
                            it.state,
                            program,
                            false,
                        )
                    )
                }
            }
            conclusion {
                left { unfolded && state is TypeState && !state.isDroppable && !state.isEnd }
                right { it.reduce { env1, env2 -> merge(env1, env2) } }
            }
        }

    }