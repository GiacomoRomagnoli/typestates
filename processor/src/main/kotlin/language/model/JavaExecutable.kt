package language.model

import annotations.Requires
import com.sun.source.util.TreePath
import language.types.Bool
import language.types.Double
import language.types.Integer
import language.types.PT
import language.types.Shared
import language.types.T
import language.types.Top
import language.types.U
import language.types.Void
import language.types.union
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

abstract class JavaExecutable(
    open val element: ExecutableElement,
    protected val program: Program,
    protected val ctx: JavaModelContext
) {
    protected fun TypeMirror.toRT(annotation: Iterable<String>? = null) =
        when(this.kind) {
            TypeKind.BOOLEAN ->  Bool
            TypeKind.INT -> Integer
            TypeKind.DOUBLE -> Double
            TypeKind.VOID -> Void
            TypeKind.DECLARED -> {
                val typeElement = ctx.types.asElement(this) as TypeElement
                when (typeElement.kind) {
                    ElementKind.ENUM -> program.toJavaEnum(typeElement)
                    ElementKind.CLASS -> {
                        val c = program[typeElement.qualifiedName.toString()] ?: TODO()
                        if (annotation == null)
                            c at Shared
                        else if (c.isLinear)
                            c at (annotation
                                .map { c.protocol!![it]?.let { u -> U(u) } ?: Top }
                                .reduceOrNull (T::union) ?: Top)
                        else
                            c at Top
                    }
                    else -> TODO("not supported type $this")
                }
            }
            else -> TODO("not supported type $this")
        }

    val body by lazy {
        val declaration = ctx.trees.getTree(element) ?: return@lazy null
        val path = ctx.trees.getPath(element) ?: return@lazy null
        TreePath(path, declaration.body)
    }

    val pt by lazy {
        element.parameters.map { parameter ->
            val annotation = parameter.getAnnotation(Requires::class.java)?.value?.toSet()
            parameter.asType().toRT(annotation) as PT
        }
    }
}