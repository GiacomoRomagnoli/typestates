package processor

import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CompilationTest: FunSpec({
    test("Greeter") {
        val greeter = JavaFileObjects.forResource("classes/greeter/Greeter.java")
        val lifecycle = JavaFileObjects.forResource("classes/greeter/GreeterLifecycle.java")
        val compilation = Compiler.javac()
            .withProcessors(TypestateProcessor())
            .compile(greeter, lifecycle)
        compilation.diagnostics().forEach { println(it) }
        compilation.status() shouldBe Compilation.Status.SUCCESS
    }

    test("Greeter reverse") {
        val greeter = JavaFileObjects.forResource("classes/greeter/Greeter.java")
        val lifecycle = JavaFileObjects.forResource("classes/greeter/GreeterReverseLifecycle.java")
        val compilation = Compiler.javac()
            .withProcessors(TypestateProcessor())
            .compile(greeter, lifecycle)
        compilation.diagnostics().forEach { println(it) }
        compilation.status() shouldBe Compilation.Status.FAILURE
    }

    test("garbage Greeter") {
        val greeter = JavaFileObjects.forResource("classes/greeter/Greeter.java")
        val lifecycle = JavaFileObjects.forResource("classes/greeter/GarbageGreeter.java")
        val compilation = Compiler.javac()
            .withProcessors(TypestateProcessor())
            .compile(greeter, lifecycle)
        compilation.diagnostics().forEach { println(it) }
        compilation.status() shouldBe Compilation.Status.FAILURE
    }
})