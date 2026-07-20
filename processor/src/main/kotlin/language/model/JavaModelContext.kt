package language.model

import com.sun.source.util.Trees
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class JavaModelContext(val types: Types, val elements: Elements, val trees: Trees) {
    companion object {
        fun from(env: ProcessingEnvironment) =
            JavaModelContext(
                types = env.typeUtils,
                elements = env.elementUtils,
                trees = Trees.instance(env)
            )
    }
}
