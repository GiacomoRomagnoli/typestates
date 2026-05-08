import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import semantic.Protocol.parse
import semantic.Protocol.protIn
import semantic.Protocol.validate
import semantic.SemanticException

class SemanticTest: FunSpec({

    test("symbols must be uniquely defined") {
        val protocol = """
            typestate A { 
                S = { init() : N, init2() : N }
                S = { m(int) : end }
            }
        """.trimIndent()
        val ast = parse(protocol)
        shouldThrow<SemanticException> { ast.validate() }
    }

    test("symbols must be declared to be referred to") {
        val protocol = """
            typestate A { 
                S = { init() : N, init2() : N }
                M = { m(int) : end }
            }
        """.trimIndent()
        val ast = parse(protocol)
        shouldThrow<SemanticException> { ast.validate() }
    }

    test("successful validation simply returns the ast for continues passing style") {
        val protocol = """
            typestate A { 
                S = { init() : N, init2() : N }
                N = { m(int) : end }
            }
        """.trimIndent()
        val ast = parse(protocol)
        shouldNotThrow<SemanticException> { ast.validate() shouldBe ast }
    }

    test("protIn returns only reachable states") {
        val protocol = """
            typestate A { 
                S = { init(): N, init2(): N }
                N = { m1(): end, m2(): L }
                L = { m3(): <true: N, false: L> }
                B = { m4(): end }
            }
        """.trimIndent()
        val states = parse(protocol).validate().protIn()
        states.size shouldBe 4
        states.map { it.name.value }.toSet() shouldBe setOf("S", "N", "end", "L")
    }
})