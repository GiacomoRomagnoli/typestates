package language.model

import annotations.Requires
import com.sun.source.util.TreePath
import language.types.PT
import language.types.resolve
import javax.lang.model.element.ExecutableElement

abstract class JavaExecutable(
    open val element: ExecutableElement,
    protected val program: Program,
    protected val ctx: JavaModelContext
) {
    val body: JavaBody? by lazy {
        val declaration = ctx.trees.getTree(element) ?: return@lazy null
        val declarationPath = ctx.trees.getPath(element) ?: return@lazy null
        val block = declaration.body ?: return@lazy null
        JavaBody(block, TreePath(declarationPath, block))
    }

    val pt by lazy {
        element.parameters.map { parameter ->
            val annotation = parameter.getAnnotation(Requires::class.java)?.value
            parameter.asType().resolve(ctx, annotation, program) as PT
        }
    }
}