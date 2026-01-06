package hny2026.tests

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import chisel3.simulator.HasSimulator
import svsim.verilator.Backend.CompilationSettings
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random

import hny2026._

/**
 * Run all tests:
 * $ mill hny2026.test
 *
 * Run only this test:
 * $ mill hny2026.test.testOnly hny2026.tests.StrobeGeneratorTest
 */
class StrobeGeneratorTest extends AnyFlatSpec with ChiselSim {
  val clockFreq = 10000

  // Enable tracing (see: https://github.com/chipsalliance/chisel/discussions/3957)
  implicit val verilator: HasSimulator = HasSimulator.simulators
    .verilator(verilatorSettings =
      CompilationSettings.default.withTraceStyle(
        Some(
          CompilationSettings.TraceStyle(
            kind = CompilationSettings.TraceKind.Vcd))))

  /**
   * Use Chisel testbench because ChiselSim don't support fork-join constructions.
   *
   * From documentation (https://www.chisel-lang.org/docs/appendix/migrating-from-chiseltest):
   * > ChiselSim also does not currently have any support for fork-join, so any tests
   * > using those constructs will need to be rewritten in a single-threaded manner.
   *
   * @param frameRate
   */
  class StrobeGeneratorTB(frameRate: Double) extends Module {
    val io = IO(new Bundle {
      val duration = Input(UInt(32.W))
      val strobeCount = Output(UInt(32.W))
      val start = Input(Bool())
      val done = Output(Bool())
    })

    val strobe = StrobeGenerator(HnyConfig(clockFreq, frameRate))
    val cycles = RegInit(chiselTypeOf(io.duration), 0.U)
    val strobes = RegInit(chiselTypeOf(io.strobeCount), 0.U)
    val count = RegInit(false.B)
    val done = RegInit(false.B)

    io.strobeCount := strobes
    io.done := done

    when(!done) {
      when(count) {
        when(cycles === io.duration) {
          count := false.B
          done := true.B
        }
      } otherwise {
        when(io.start) {
          count := true.B
        }
      }
    }

    when(count) {
      cycles := cycles + 1.U
      when(strobe) {
        strobes := strobes + 1.U
      }
    }
  }

  /**
   * TODO: Add description
   *
   * @param dut
   * @param frameRate
   * @return
   */
  def check(dut: StrobeGeneratorTB)(implicit frameRate: Double): Unit = {
    val scale = if ((clockFreq / frameRate).isWhole) 10 else 100
    val expect = (frameRate * scale).round.toInt
    val duration = (clockFreq * scale).toInt

    enableWaves()
    dut.io.duration.poke(duration)
    dut.io.start.poke(true)
    dut.clock.stepUntil(dut.io.done, 1, duration + 10)

    val strobes = dut.io.strobeCount.peek().litValue

    println(s"$strobes strobes counted. Expected [${expect-1}..${expect+1}]")
    assert((strobes >= expect - 1) && (strobes <= expect + 1))
  }

  behavior of "StrobeGenerator"

  it should "produce 25 strobes per second" in {
    implicit val frameRate = 25.0
    simulate(new StrobeGeneratorTB(frameRate))(check)
  }

  it should "produce 30 strobes per second" in {
    implicit val frameRate = 30.0
    simulate(new StrobeGeneratorTB(frameRate))(check)
  }

  it should "produce 30.1 strobes per second" in {
    implicit val frameRate = 30.1
    simulate(new StrobeGeneratorTB(frameRate))(check)
  }

  it should "produce 30.4 strobes per second" in {
    implicit val frameRate = 30.4
    simulate(new StrobeGeneratorTB(frameRate))(check)
  }
}

/**
 * Run only this test:
 * $ mill hny2026.test.testOnly hny2026.tests.CharSenderTest
 */
class CharSenderTest extends AnyFlatSpec with ChiselSim {
  val cfg = HnyConfig(1000, 25)
  val dataLength = 10

  // Enable tracing (see: https://github.com/chipsalliance/chisel/discussions/3957)
  implicit val verilator: HasSimulator = HasSimulator.simulators
    .verilator(verilatorSettings =
      CompilationSettings.default.withTraceStyle(
        Some(
          CompilationSettings.TraceStyle(
            kind = CompilationSettings.TraceKind.Vcd))))

  behavior of "CharSender"

  it should s"send $dataLength random bytes" in {
    simulate(new CharSender(cfg)) { dut =>
      enableWaves()
      val data = List.fill(dataLength)(Random.nextInt(Math.pow(2, cfg.dataWidth).toInt))

      data.foreach { x =>
        dut.io.data.bits.poke(x)
        dut.io.data.valid.poke(true.B)
        dut.clock.step()
        dut.clock.stepUntil(dut.io.data.ready, 1, 1000)
        dut.io.data.valid.poke(false.B)
      }

      dut.clock.step((cfg.clockFreq / cfg.frameRate * cfg.dataWidth * 2).toInt)
      println("See results in the wave diagram.")
    }
  }
}

/**
 * Run only this test:
 * $ mill hny2026.test.testOnly hny2026.tests.HNY2026Test
 */
class HNY2026Test extends AnyFlatSpec with ChiselSim {
  val cfg = HnyConfig(1000, 25)
  val str = "Hello!"

  // Enable tracing (see: https://github.com/chipsalliance/chisel/discussions/3957)
  implicit val verilator: HasSimulator = HasSimulator.simulators
    .verilator(verilatorSettings =
      CompilationSettings.default.withTraceStyle(
        Some(
          CompilationSettings.TraceStyle(
            kind = CompilationSettings.TraceKind.Vcd))))

  behavior of "HNY2026"

  it should s"send string '$str'" in {
    simulate(new HNY2026(cfg, str)) { dut =>
      enableWaves()
      dut.clock.step((cfg.clockFreq / cfg.frameRate * cfg.dataWidth * str.length() * 2).toInt)
      println("See results in the wave diagram.")
    }
  }
}
