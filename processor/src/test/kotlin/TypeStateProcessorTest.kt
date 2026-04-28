import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TypeStateProcessorTest: StringSpec({
    "correctly invoked" {
        val source = JavaFileObjects.forSourceString(
            "TestClass",
            """
                @Typestate(protocol = "myprotocol")
                class TestClass {
                    void m1(@Requires(t = "T1") TestClass x) {}
                    @Ensures(t = "T2") void m2() {}
                } 
            """.trimIndent()
        )
        val compilation = Compiler.javac()
            .withProcessors(TypeStateProcessor())
            .compile(source)
        compilation.diagnostics().forEach {
            println(it)
        }
        compilation.status() shouldBe Compilation.Status.SUCCESS
    }
})