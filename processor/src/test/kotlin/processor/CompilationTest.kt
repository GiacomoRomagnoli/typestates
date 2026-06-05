package processor

import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CompilationTest: FunSpec({
    test("Greeter") {
        val source = JavaFileObjects.forResource("classes/Greeter.java")
        val compilation = Compiler.javac()
            .withProcessors(Processor())
            .compile(source)
        compilation.diagnostics().forEach { println(it) }
        compilation.status() shouldBe Compilation.Status.SUCCESS
    }

    test("Car") {
        val source = JavaFileObjects.forResource("classes/Car.java")
        val compilation = Compiler.javac()
            .withProcessors(Processor())
            .compile(source)
        compilation.diagnostics().forEach { println(it) }
        compilation.status() shouldBe Compilation.Status.SUCCESS
    }
})