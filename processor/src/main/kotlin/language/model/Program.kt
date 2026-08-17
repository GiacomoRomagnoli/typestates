package language.model

import annotations.Typestate
import com.sun.source.util.TreePath
import com.sun.source.util.Trees
import processor.Loader
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class Program(
    val trees: Trees,
    val elements: Elements,
    val types: Types,
    private val loader: Loader
) {
    private val classes = mutableMapOf<String, JavaClass>()
    private val enums = mutableMapOf<String, JavaEnum>()

    fun classByElement(element: Element): JavaClass {
        require(element.kind == ElementKind.CLASS)
        val element = element as TypeElement
        return classes.getOrPut(element.qualifiedName.toString()) {
            JavaClass(
                element,
                element.getAnnotation(Typestate::class.java)
                    ?.value
                    ?.let(loader::load),
                this
            )
        }
    }

    fun classByPath(path: TreePath) =
        trees.getElement(path)?.takeIf { it.kind == ElementKind.CLASS }?.let(::classByElement)

    fun constructorByPath(path: TreePath): JavaConstructor? {
        val element = trees.getElement(path) as? ExecutableElement
        return element
            ?.takeIf { it.kind == ElementKind.CONSTRUCTOR }
            ?.let { it.enclosingElement as? TypeElement }
            ?.let(::classByElement)
            ?.constructors
            ?.singleOrNull { it.element == element }
    }

    fun methodByPath(path: TreePath): JavaMethod? {
        val element = trees.getElement(path) as? ExecutableElement
        return element
            ?.takeIf { it.kind == ElementKind.METHOD }
            ?.enclosingElement
            ?.let(::classByElement)
            ?.meths
            ?.singleOrNull { it.element == element }
    }

    fun enumByElement(element: Element) : JavaEnum {
        require(element.kind == ElementKind.ENUM)
        val element = element as TypeElement
        return enums.getOrPut(element.qualifiedName.toString()) { JavaEnum(element) }
    }

    fun enumByValuePath(path: TreePath) =
        (trees.getElement(path) as? VariableElement)
            ?.takeIf { it.kind == ElementKind.ENUM_CONSTANT }
            ?.let { it.enclosingElement as? TypeElement }
            ?.let(::enumByElement)

    fun enumByTypePath(path: TreePath) =
        (trees.getElement(path) as? TypeElement)
            ?.takeIf { it.kind == ElementKind.ENUM }
            ?.let(::enumByElement)
}