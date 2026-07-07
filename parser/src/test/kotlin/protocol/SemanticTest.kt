package protocol

import ast.parse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SemanticTest: FunSpec({

    test("symbols must be uniquely defined") {
        val protocol = """
            typestate A { 
                S = { init() : S, init2() : S }
                S = { m(int) : end }
            }
        """.trimIndent()
        val ast = parse(protocol)
        ast.analyse().size shouldBe 1
    }

    test("symbols must be declared to be referred to") {
        val protocol = """
            typestate A { 
                S = { init() : N, init2() : M }
                M = { m(int) : end }
            }
        """.trimIndent()
        val ast = parse(protocol)
        ast.analyse().size shouldBe 1
    }

    test("transitions must be deterministic") {
        val protocol = """
            typestate A { 
                S = { init(int) : N, init(int) : <true:N, false:S> }
                N = { m(int) : end }
            }
        """.trimIndent()
        val ast = parse(protocol)
        ast.analyse().size shouldBe 1
    }

    test("analysis without semantic errors") {
        val protocol = """
            typestate A { 
                S = { init() : N, init2() : N }
                N = { m(int) : end }
            }
        """.trimIndent()
        val ast = parse(protocol)
        ast.analyse().size shouldBe 0
    }
})