package language.model

import javax.lang.model.element.ExecutableElement

class JavaConstructor(
    override val element: ExecutableElement,
    program: Program,
    context: JavaModelContext
) : JavaExecutable(element, program, context)