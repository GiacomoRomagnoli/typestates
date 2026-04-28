import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class TypeStateProcessor: AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(
        Ensures::class.java.canonicalName,
        Requires::class.java.canonicalName,
        Typestate::class.java.canonicalName,
    )

    override fun process(annotations: Set<TypeElement?>?, roundEnv: RoundEnvironment?): Boolean {
        roundEnv?.getElementsAnnotatedWith(Typestate::class.java)?.forEach {
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "${it.simpleName} (${it.kind})")
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "${it.getAnnotation(Typestate::class.java)}")
        }
        roundEnv?.getElementsAnnotatedWith(Requires::class.java)?.forEach {
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "${it.simpleName} (${it.kind})")
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "${it.getAnnotation(Requires::class.java)}")
        }
        roundEnv?.getElementsAnnotatedWith(Ensures::class.java)?.forEach {
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "${it.simpleName} (${it.kind})")
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "${it.getAnnotation(Ensures::class.java)}")
        }
        return true
    }
}