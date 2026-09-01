package rules

import language.model.JavaClass
import language.model.Program
import language.model.anytime
import language.types.FieldId
import language.types.TypeEnv
import language.types.isLinear
import language.types.term
import language.types.toTC
import rules.dsl.Judgement
import rules.dsl.Traceable
import rules.dsl.judgement
import rules.utils.chkOvr
import rules.utils.chkProt

object Clss {
    data class Left(
        val clazz: JavaClass,
        val program: Program,
        val extends: Boolean = clazz.superclass?.qualifiedName != "java.lang.Object",
    ) : Traceable {
        override fun trace() = clazz.qualifiedName
    }
}

val CLASS_JUDGMENT: Judgement<Clss.Left, Unit> = judgement {
    rule("TClass") {
        premise {
            ensure(chkProt(clazz))
            clazz.constructors
                .map { b -> CONSTRUCTOR_JUDGMENT.derive(Cns(b, program)) }
                .forEach { tf ->
                    val tf1 = TYPESTATE_DEFINITION_JUDGMENT.derive(
                        TypeStateDef.Left(
                            emptyMap(),
                            tf,
                            clazz,
                            clazz.protocol!!.initState,
                            program
                        )
                    )
                    ensure(term(tf1))
                }
            val tf: TypeEnv = buildMap {
                clazz.allFields.filterNot { it.jt.isLinear }
                    .forEach { put(FieldId(it.owner, it.name), toTC(it.jt)) }
            }
            clazz.meths
                .filter { m -> anytime(clazz, m.pSig) }
                .forEach { m ->
                    val tf1 = METHOD_JUDGEMENT.derive(Meth.Left(tf, m, program, clazz))
                    ensure(tf == tf1)
                }
        }
        conclusion {
            left { clazz.isLinear && !extends }
            right { }
        }
    }

    rule("TClassNL") {
        premise {
            ensure(clazz.allFields.none { it.jt.isLinear })
            clazz.constructors.forEach { CONSTRUCTOR_JUDGMENT.derive(Cns(it, program)) }
            val tf: TypeEnv = buildMap {
                clazz.allFields.forEach { put(FieldId(it.owner, it.name), toTC(it.jt)) }
            }
            clazz.meths.forEach {
                val tf1 = METHOD_JUDGEMENT.derive(Meth.Left(tf, it, program, clazz))
                ensure(tf == tf1)
            }
        }
        conclusion {
            left { !clazz.isLinear && !extends }
            right { }
        }
    }

    rule("TExt") {
        premise {
            val superClass = clazz.superclass ?: fail()
            ensure(superClass.protocol == null || clazz.protocol!! sub superClass.protocol!!)
            ensure(chkOvr(clazz, superClass))
            CLASS_JUDGMENT.derive(copy(extends = false))
        }
        conclusion {
            left { extends }
            right { }
        }
    }
}