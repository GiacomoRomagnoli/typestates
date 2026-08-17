package rules

import com.sun.source.tree.AssignmentTree
import com.sun.source.tree.BinaryTree
import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.InstanceOfTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.tree.NewClassTree
import com.sun.source.tree.PrimitiveTypeTree
import com.sun.source.tree.Tree
import com.sun.source.tree.TypeCastTree
import com.sun.source.tree.UnaryTree
import com.sun.source.util.TreePath
import language.model.JavaClass
import language.model.Program
import language.model.anytime
import language.model.isSubClassOf
import language.types.Bool
import language.types.BoolUnd
import language.types.Delta
import language.types.Double
import language.types.DoubleUnd
import language.types.Eid
import language.types.EnumType
import language.types.Integer
import language.types.IntegerUnd
import language.types.Null
import language.types.Shared
import language.types.TC
import language.types.THIS
import language.types.Top
import language.types.TypeEnv
import language.types.TypeStateTree
import language.types.U
import language.types.alias
import language.types.dcastTT
import language.types.defined
import language.types.evoTTI
import language.types.evolve
import language.types.Tf
import language.types.lookup
import language.types.not
import language.types.resolve
import language.types.sub
import language.types.term
import language.types.toTC
import language.types.tt
import language.types.ucastTT
import language.types.upd
import language.types.Ts
import rules.dsl.Judgement
import rules.dsl.judgement
import rules.utils.isLabel
import rules.utils.toEid
import javax.lang.model.type.TypeKind

/**
 * data types for partial result of rules
 */
data class TNew(val c: JavaClass, val delta: Delta)
data class TUpdB(val c: JavaClass, val eid: Eid, val tc: TC, val delta: Delta)
data class TUpdO(val c: JavaClass, val eid: Eid, val tt1: TypeStateTree, val tt2: TypeStateTree, val delta: Delta)
data class TUpdExt(val tc: TC, val delta: Delta)
typealias TCastB = TUpdExt
data class TUCastO(val c: JavaClass, val tt: TypeStateTree, val delta: Delta)
typealias TDCastO = TUCastO
data class TCall(val c: JavaClass, val eid: Eid, val tc: TC, val tt: TypeStateTree, val delta: Delta)
typealias TAnyt = TCall
typealias TAnytM = TUpdExt
data class TEqL(val l: String, val fields: TypeEnv, val variables: TypeEnv)

/**
 * data classes for input and output of the judgment
 */
object Expr {
    data class Left(
        val Tf: TypeEnv,
        val Ts: TypeEnv,
        val path: TreePath,
        val a: Boolean,
        val program: Program
    ) {
        val expression get() = path.leaf
    }
    data class Right(
        val tc: TC,
        val Tf: TypeEnv,
        val Ts: TypeEnv
    ) {
        constructor(tc: TC, delta: Delta) : this(tc, delta.Tf, delta.Ts)
    }
}

/**
 * Judgment for expressions
 */
val EXPRESSION_JUDGMENT: Judgement<Expr.Left, Expr.Right> = judgement {

    rule("TVal") {
        premise { VALUE_JUDGMENT.derive(Value(path, program)) }
        conclusion {
            left { expression is LiteralTree || expression is MemberSelectTree }
            right { Expr.Right(it, Tf, Ts) }
        }
    }

    rule("TId") {
        premise { IDENTIFIER_JUDGEMENT.derive(this) }
        conclusion {
            left { (expression as? ExpressionTree)?.toEid() != null }
            right { it }
        }
    }

    rule<TNew>("TNew") {
        premise {
            val newExpr = expression as NewClassTree
            val c = program.classByPath(TreePath(path, newExpr.identifier)) ?: fail()
            ensure(a || c.protocol?.let { term(U(it.initState)) } ?: true)
            val args = newExpr.arguments.map { TreePath(path, it) }
            val exprSeqL = ExprSeq.Left(Tf, Ts, true, program, args)
            val exprSeqR = EXPRESSION_SEQUENCE_JUDGMENT.derive(exprSeqL)
            val constructor = program.constructorByPath(path) ?: fail()
            ensure(exprSeqR.tcs.zip(constructor.pt).all { (tc, pt) -> tc sub toTC(pt.type) })
            TNew(c, exprSeqR.Tf to exprSeqR.Ts)
        }
        conclusion {
            left { expression is NewClassTree }
            right { Expr.Right(tt(it.c, it.c.protocol?.let { U(it.initState) } ?: Shared), it.delta) }
        }
    }

    rule<TUpdB>("TUpdB") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val assignment = expression as AssignmentTree
            val e = TreePath(path, assignment.expression)
            val exprL = Expr.Left(Tf, Ts, e, true, program)
            val exprR = EXPRESSION_JUDGMENT.derive(exprL)
            ensure(exprR.tc !is TypeStateTree)
            val eid = assignment.variable.toEid() ?: fail()
            val lkp = lookup(c, eid, exprR.Tf to exprR.Ts) ?: fail()
            ensure(lkp in listOf(Bool, BoolUnd, Integer, IntegerUnd, Double, DoubleUnd) || lkp is EnumType)
            ensure(exprR.tc sub lkp.defined())
            TUpdB(c, eid, exprR.tc, exprR.Tf to exprR.Ts)
        }
        conclusion {
            left { (expression as? AssignmentTree)?.variable?.toEid() != null }
            right { Expr.Right(it.tc, upd(it.c, it.eid, it.tc, it.delta)) }
        }
    }

    rule<TUpdO>("TUpdO") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val assignment = expression as AssignmentTree
            val e = TreePath(path, assignment.expression)
            val exprL = Expr.Left(Tf, Ts, e, true, program)
            val exprR = EXPRESSION_JUDGMENT.derive(exprL)
            val tt1 = exprR.tc as? TypeStateTree ?: fail()
            val eid = assignment.variable.toEid() ?: fail()
            val delta = exprR.Tf to exprR.Ts
            val tt = lookup(c, eid, delta) as? TypeStateTree ?: fail()
            ensure(term(tt))
            ensure(tt1.clazz isSubClassOf tt.clazz)
            val cl = tt.clazz as? JavaClass ?: fail()
            val tt2 = ucastTT(tt1, cl)
            TUpdO(c, eid, tt1, tt2, delta)
        }
        conclusion {
            left { (expression as? AssignmentTree)?.variable?.toEid() != null }
            right { Expr.Right(alias(it.tt1), upd(it.c, it.eid, it.tt2, it.delta)) }
        }
    }

    rule<TUpdExt>("TUpdExt") {
        premise {
            val assignment = expression as AssignmentTree
            val field = TreePath(path, assignment.variable)
            val fieldL = Expr.Left(Tf, Ts, field, false, program)
            val fieldR = EXPRESSION_JUDGMENT.derive(fieldL)
            ensure(fieldR.Tf == Tf && fieldR.Ts == Ts)
            val value = TreePath(path, assignment.expression)
            val valueL = Expr.Left(Tf, Ts, value, true, program)
            val valueR = EXPRESSION_JUDGMENT.derive(valueL)
            ensure(valueR.tc sub fieldR.tc)
            TUpdExt(valueR.tc, valueR.Tf to valueR.Ts)
        }
        conclusion {
            left {
                ((expression as? AssignmentTree)?.variable as? MemberSelectTree)
                    ?.expression
                    ?.toEid() != null
            }
            right { Expr.Right(it.tc, it.delta) }
        }
    }

    rule<TCastB>("TCastB") {
        premise {
            val cast = expression as TypeCastTree
            val e = TreePath(path, cast.expression)
            val eL = Expr.Left(Tf, Ts, e, a, program)
            val eR = EXPRESSION_JUDGMENT.derive(eL)
            ensure(eR.tc in setOf(Bool, Integer, Double))
            val b = when((cast.type as PrimitiveTypeTree).primitiveTypeKind) {
                TypeKind.BOOLEAN -> Bool
                TypeKind.INT -> Integer
                TypeKind.DOUBLE -> Double
                else -> fail()
            }
            ensure(b sub eR.tc || eR.tc sub b)
            TCastB(b, eR.Tf to eR.Ts)
        }
        conclusion {
            left {
                ((expression as? TypeCastTree)?.type as? PrimitiveTypeTree)
                    ?.primitiveTypeKind in setOf(TypeKind.BOOLEAN, TypeKind.INT, TypeKind.DOUBLE)
            }
            right { Expr.Right(it.tc, it.delta) }
        }
    }

    rule<TUCastO>("TUCastO") {
        premise {
            val cast = expression as TypeCastTree
            val e = TreePath(path, cast.expression)
            val eL = Expr.Left(Tf, Ts, e, a, program)
            val eR = EXPRESSION_JUDGMENT.derive(eL)
            val tt = eR.tc as? TypeStateTree ?: fail()
            val c = program.classByPath(TreePath(path, cast.type)) ?: fail()
            ensure(tt.clazz isSubClassOf c)
            TUCastO(c, tt, eR.Tf to eR.Ts)
        }
        conclusion {
            left {
                (expression as? TypeCastTree)
                    ?.let{ program.classByPath(TreePath(path, it.type)) } != null
            }
            right { Expr.Right(ucastTT(it.tt, it.c), it.delta) }
        }
    }

    rule<TDCastO>("TDCastO") {
        premise {
            val cast = expression as TypeCastTree
            val e = TreePath(path, cast.expression)
            val eL = Expr.Left(Tf, Ts, e, a, program)
            val eR = EXPRESSION_JUDGMENT.derive(eL)
            val tt = eR.tc as? TypeStateTree ?: fail()
            val c = program.classByPath(TreePath(path, cast.type)) ?: fail()
            ensure(c isSubClassOf tt.clazz)
            ensure(c != tt.clazz)
            TDCastO(c, tt, eR.Tf to eR.Ts)
        }
        conclusion {
            left {
                (expression as? TypeCastTree)?.let{ program.classByPath(TreePath(path, it.type)) } != null
            }
            right { Expr.Right(dcastTT(it.tt, it.c), it.delta) }
        }
    }

    rule<TCall>("TCall") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val invocation = expression as MethodInvocationTree
            val select = invocation.methodSelect as MemberSelectTree
            val receiver = TreePath(TreePath(path, select), select.expression)
            val receiverL = Expr.Left(Tf, Ts, receiver, true, program)
            val receiverR = EXPRESSION_JUDGMENT.derive(receiverL)
            val tt = receiverR.tc as? TypeStateTree ?: fail()
            val args = invocation.arguments.map { TreePath(path, it) }
            val argsL = ExprSeq.Left(receiverR.Tf, receiverR.Ts, true, program, args)
            val argsR = EXPRESSION_SEQUENCE_JUDGMENT.derive(argsL)
            val method = program.methodByPath(path) ?: fail()
            ensure(argsR.tcs.size == method.pt.size)
            ensure(argsR.tcs.indices.all { i -> argsR.tcs[i] sub toTC(method.pt[i].type) })
            val tt1 = evoTTI(tt, method.pSig)
            ensure(!(Top sub tt1.type))
            val tc = toTC(method.rt)
            ensure(a || term(tc))
            val eid = select.expression.toEid() ?: fail()
            TCall(c, eid, tc, tt1, argsR.Tf to argsR.Ts)
        }
        conclusion {
            left {
                ((expression as? MethodInvocationTree)?.methodSelect as? MemberSelectTree)
                    ?.expression
                    ?.toEid() != null
            }
            right { Expr.Right(it.tc, upd(it.c, it.eid, it.tt, it.delta)) }
        }
    }

    rule<TAnyt>("TAnyt") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val invocation = expression as MethodInvocationTree
            val select = invocation.methodSelect as MemberSelectTree
            val receiver = TreePath(TreePath(path, select), select.expression)
            val receiverL = Expr.Left(Tf, Ts, receiver, true, program)
            val receiverR = EXPRESSION_JUDGMENT.derive(receiverL)
            val tt = receiverR.tc as? TypeStateTree ?: fail()
            val args = invocation.arguments.map { TreePath(path, it) }
            val argsL = ExprSeq.Left(receiverR.Tf, receiverR.Ts, true, program, args)
            val argsR = EXPRESSION_SEQUENCE_JUDGMENT.derive(argsL)
            val method = program.methodByPath(path) ?: fail()
            ensure(argsR.tcs.size == method.pt.size)
            ensure(argsR.tcs.indices.all { i -> argsR.tcs[i] sub toTC(method.pt[i].type) })
            ensure(anytime(tt.clazz, method.pSig))
            val tc = toTC(method.rt)
            ensure(a || term(tc))
            ensure(!(Null sub tt.type))
            val eid = select.expression.toEid() ?: fail()
            TAnyt(c, eid, tc, tt, argsR.Tf to argsR.Ts)
        }
        conclusion {
            left {
                ((expression as? MethodInvocationTree)?.methodSelect as? MemberSelectTree)
                    ?.expression
                    ?.toEid() != null
            }
            right { Expr.Right(it.tc, upd(it.c, it.eid, it.tt, it.delta)) }
        }
    }

    rule<TAnytM>("TAnytM") {
        premise {
            val c = (Ts[THIS] as TypeStateTree).clazz as JavaClass
            val invocation = expression as MethodInvocationTree
            val args = invocation.arguments.map { TreePath(path, it) }
            val argsL = ExprSeq.Left(Tf, Ts, true, program,args)
            val argsR = EXPRESSION_SEQUENCE_JUDGMENT.derive(argsL)
            val method = program.methodByPath(path) ?: fail()
            ensure(argsR.tcs.size == method.pt.size)
            ensure(argsR.tcs.indices.all { i -> argsR.tcs[i] sub toTC(method.pt[i].type) })
            ensure(anytime(c, method.pSig))
            val tc = toTC(method.rt)
            ensure(a || term(tc))
            TAnytM(tc, argsR.Tf to argsR.Ts)
        }
        conclusion {
            left {
                val invocation = expression as? MethodInvocationTree
                val select = invocation?.methodSelect
                select is IdentifierTree ||
                        ((select as? MemberSelectTree)?.expression as? IdentifierTree)
                            ?.name.toString() in setOf("this", "super")
            }
            right { Expr.Right(it.tc, it.delta) }
        }
    }

    rule<Delta>("TNot") {
        premise {
            val not = expression as UnaryTree
            val e = not.expression
            val judgement = EXPRESSION_JUDGMENT.derive(copy(path = TreePath(path, e), a = false))
            ensure(judgement.tc is Bool)
            judgement.Tf to judgement.Ts
        }
        conclusion {
            left { expression.kind == Tree.Kind.LOGICAL_COMPLEMENT }
            right { Expr.Right(Bool, !it.Tf, !it.Ts) }
        }
    }

    rule<Delta>("TEq") {
        premise {
            val equals = expression as BinaryTree
            val e1 = EXPRESSION_JUDGMENT.derive(copy(path = TreePath(path, equals.leftOperand), a = false))
            ensure(e1.tc !is TypeStateTree)
            val e2Left =
                Expr.Left(
                    resolve(e1.Tf),
                    resolve(e1.Ts),
                    TreePath(path, equals.rightOperand),
                    false,
                    program
                )
            val e2Right = EXPRESSION_JUDGMENT.derive(e2Left)
            ensure(e2Right.tc !is TypeStateTree)
            ensure(e1.tc sub e2Right.tc || e2Right.tc sub e1.tc)
            e2Right.Tf to e2Right.Ts
        }
        conclusion {
            left {
                val equals = expression as? BinaryTree ?: return@left false
                expression.kind == Tree.Kind.EQUAL_TO &&
                        !isLabel(TreePath(path, equals.leftOperand)) &&
                        !isLabel(TreePath(path, equals.rightOperand))
            }
            right { Expr.Right(Bool, resolve(it.Tf), resolve(it.Ts)) }
        }
    }

    rule<TEqL>("TEqL") {
        premise {
            val equals = expression as BinaryTree
            val l = (equals.rightOperand as? MemberSelectTree)?.identifier?.toString()
                ?: (equals.rightOperand as LiteralTree).value.toString()
            val e = EXPRESSION_JUDGMENT.derive(copy(path = TreePath(path, equals.leftOperand), a = false))
            val tc = VALUE_JUDGMENT.derive(Value(TreePath(path, equals.rightOperand), program))
            ensure(e.tc == tc)
            TEqL(l, e.Tf, e.Ts)
        }
        conclusion {
            left {
                val equals = expression as? BinaryTree ?: return@left false
                expression.kind == Tree.Kind.EQUAL_TO &&
                        !isLabel(TreePath(path, equals.leftOperand)) &&
                        isLabel(TreePath(path, equals.rightOperand))
            }
            right {
                Expr.Right(
                    Bool,
                    evolve(it.fields, it.l),
                    evolve(it.variables, it.l)
                )
            }
        }
    }

    rule<TEqL>("TEqL2") {
        premise {
            val equals = expression as BinaryTree
            val l = (equals.leftOperand as? MemberSelectTree)?.identifier?.toString()
                ?: (equals.leftOperand as LiteralTree).value.toString()
            val e = EXPRESSION_JUDGMENT.derive(copy(path = TreePath(path, equals.rightOperand), a = false))
            val tc = VALUE_JUDGMENT.derive(Value(TreePath(path, equals.leftOperand), program))
            ensure(e.tc == tc)
            TEqL(l, e.Tf, e.Ts)
        }
        conclusion {
            left {
                val equals = expression as? BinaryTree ?: return@left false
                expression.kind == Tree.Kind.EQUAL_TO &&
                        isLabel(TreePath(path, equals.leftOperand)) &&
                        !isLabel(TreePath(path, equals.rightOperand))
            }
            right {
                Expr.Right(
                    Bool,
                    evolve(it.fields, it.l),
                    evolve(it.variables, it.l)
                )
            }
        }
    }

    rule("TInst") {
        premise {
            val instanceOf = expression as InstanceOfTree
            val e = EXPRESSION_JUDGMENT.derive(
                copy(path = TreePath(path, instanceOf.expression), a = false)
            )
            val c = program.classByPath(TreePath(path,instanceOf.type)) ?: fail()
            ensure(e.tc is TypeStateTree)
            ensure((e.tc as TypeStateTree).clazz isSubClassOf c)
            e.Tf to e.Ts
        }
        conclusion {
            left { expression.kind == Tree.Kind.INSTANCE_OF }
            right { Expr.Right(Bool, it ) }
        }
    }
}