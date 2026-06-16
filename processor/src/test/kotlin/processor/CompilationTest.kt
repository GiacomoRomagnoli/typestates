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

    test("Suv") {
        val mode = JavaFileObjects.forResource("enums/Mode.java")
        val car = JavaFileObjects.forResource("classes/Car.java")
        val suv = JavaFileObjects.forResource("classes/Suv.java")
        val compilation = Compiler.javac()
            .withProcessors(Processor())
            .compile(mode, car, suv)
        compilation.diagnostics().forEach { println(it) }
        compilation.status() shouldBe Compilation.Status.SUCCESS
    }

    test("SuvService") {
        val mode = JavaFileObjects.forResource("enums/Mode.java")
        val car = JavaFileObjects.forResource("classes/Car.java")
        val suv = JavaFileObjects.forResource("classes/Suv.java")
        val carService = JavaFileObjects.forResource("classes/CarService.java")
        val suvService = JavaFileObjects.forResource("classes/SuvService.java")
        val compilation = Compiler.javac()
            .withProcessors(Processor())
            .compile(mode, car, suv, carService, suvService)
        compilation.diagnostics().forEach { println(it) }
        compilation.status() shouldBe Compilation.Status.FAILURE
    }
})