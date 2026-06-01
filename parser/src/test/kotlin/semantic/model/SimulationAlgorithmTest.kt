package semantic.model

import ProtocolContext
import ast.parse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SimulationAlgorithmTest: FunSpec({
    lateinit var car: Protocol
    lateinit var suv: Protocol
    val COMF_ON = "COMF_ON"
    val SPORT_ON = "SPORT_ON"
    val ON = "ON"
    val OFF = "OFF"

    beforeTest {
        val carInput = requireNotNull(javaClass.getResource("/car.protocol")?.readText())
        val suvInput = requireNotNull(javaClass.getResource("/suv.protocol")?.readText())
        car = ProtocolContext(parse(carInput)).model
        suv = ProtocolContext(parse(suvInput)).model
    }

    test("COMF_ON simulates ON") {
        val comfOn = requireNotNull(suv[COMF_ON])
        val on = requireNotNull(car[ON])
        comfOn simulates on shouldBe true
    }

    test("SPORT_ON simulates ON") {
        val sportOn = requireNotNull(suv[SPORT_ON])
        val on = requireNotNull(car[ON])
        sportOn simulates on shouldBe true
    }

    test("OFF simulates OFF") {
        val suvOff = requireNotNull(suv[OFF])
        val carOff = requireNotNull(car[OFF])
        suvOff simulates carOff shouldBe true
    }

    test("COMF_ON doesn't simulate OFF") {
        val comfOn = requireNotNull(suv[COMF_ON])
        val off = requireNotNull(car[OFF])
        comfOn simulates off shouldBe false
    }

    test("SPORT_ON doesn't simulate OFF") {
        val sportOn = requireNotNull(suv[SPORT_ON])
        val off = requireNotNull(car[OFF])
        sportOn simulates off shouldBe false
    }

    test("ON doesn't simulate COMF_ON") {
        val comfOn = requireNotNull(suv[COMF_ON])
        val on = requireNotNull(car[ON])
        on simulates comfOn shouldBe false
    }

    test("ON doesn't simulate SPORT_ON") {
        val sportOn = requireNotNull(suv[SPORT_ON])
        val on = requireNotNull(car[ON])
        on simulates sportOn shouldBe false
    }
})