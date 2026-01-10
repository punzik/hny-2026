package hny2026

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

/**
 * Generates a single-cycle strobe pulse at a rate determined by the supplied configuration. The
 * strobe is used to time the transmission of bits from a character to the LED driver in {@link
 * CharSender}.
 *
 * <p>The strobe generator works by counting clock cycles so that a pulse occurs once every
 * <code>cntToInt</code> cycles plus a fractional adjustment. The fractional part is implemented by
 * a second counter that counts <code>fracCntTo</code> pulses before a full <code>cntToInt</code>
 * cycle is considered complete. This yields a frame rate accurate to within {@code
 * frameRateAccuracy}.</p>
 *
 * @param cfg configuration that contains the clock frequency, frame rate and accuracy target.
 */
class StrobeGenerator(cfg: HnyConfig) extends Module {
  val strobe = IO(Output(Bool()))

  val cntTo = (((cfg.clockFreq / cfg.frameRate) / cfg.frameRateAccuracy).round * cfg.frameRateAccuracy)
  val cntToInt = cntTo.round
  val cntInt = RegInit(0.U(log2Up(cntToInt+1).W))
  val cntIntDone = cntInt === 0.U
  val fracPart = cntTo - cntToInt
  val cntFracDone = Wire(Bool())

  strobe := cntIntDone

  if (fracPart != 0) {
    val fracCntTo = (1 / fracPart.abs).toInt
    val fracCnt = RegInit(0.U(log2Up(fracCntTo).W))

    when(cntIntDone) {
      when(cntFracDone) {
        fracCnt := 0.U
      } otherwise {
        fracCnt := fracCnt + 1.U
      }
    }

    cntFracDone := fracCnt === (fracCntTo-1).U
  } else {
    cntFracDone := false.B
  }

  when(cntIntDone) {
    when(cntFracDone) {
      cntInt := {
        if (fracPart > 0)
          cntToInt.U
        else
          (cntToInt - 2).U
      }
    } otherwise {
      cntInt := (cntToInt - 1).U
    }
  } otherwise {
    cntInt := cntInt - 1.U
  }
}

/**
 * Convenience constructor for a strobe generator.
 *
 * @param cfg configuration for the strobe generator.
 * @return a {@code Bool} that goes high for one cycle at the desired frame rate.
 */
object StrobeGenerator {
  def apply(cfg: HnyConfig): Bool = {
    val sg = Module(new StrobeGenerator(cfg))
    sg.strobe
  }
}

/**
 * Sends a single byte of data to the LED matrix. The module implements a 1-bit wide serial output
 * using the two signals {@code one} and {@code zero}. When the {@code strobe} from {@link
 * StrobeGenerator} goes high, the next bit of the current character is shifted out. The MSB is a
 * parity bit that is the XOR of all data bits. The last bit is a flag that indicates
 * that the character has been fully transmitted.
 *
 * @param cfg configuration that defines the data width and other timing parameters.
 */
class CharSender(cfg: HnyConfig) extends Module {
  val io = IO(new Bundle {
    val data = Flipped(Decoupled(UInt(cfg.dataWidth.W)))
    val one = Bool()
    val zero = Bool()
  })

  val strobe = StrobeGenerator(cfg)
  val send = RegInit(false.B)
  val data = Reg(Bits((cfg.dataWidth+2).W)) // Extra bits for parity and done flag
  val one = RegInit(false.B)
  val zero = RegInit(false.B)
  val parity = io.data.bits.xorR

  io.data.ready := !send
  io.one := one
  io.zero := zero

  when(send) {
    when(strobe) {
      data := data >> 1

      when(data.head(data.getWidth-1) === 0.U) {
        send := false.B
      } otherwise {
        one := data(0)
        zero := ~data(0)
      }
    }
  } otherwise {
    one := false.B
    zero := false.B

    when(io.data.valid) {
      data := true.B ## parity ## io.data.bits
      send := true.B
    }
  }
}

/**
 * Top-level module that drives the RGB LEDs to display a string. Each character of the provided
 * string is sent to the LED driver one after the other. The module uses a {@link CharSender}
 * instance for the actual bit-streaming and routes the {@code one} and {@code zero} signals to the
 * red and green LEDs respectively.
 *
 * @param cfg configuration that controls the serial timing and data width.
 * @param str string to display on the LED matrix.
 */
class HNY2026(cfg: HnyConfig, str: String) extends Module {
  val io = IO(new Bundle {
    val ledR = Output(Bool())
    val ledG = Output(Bool())
    val ledB = Output(Bool())
  })

  val sender = Module(new CharSender(cfg))
  val chars = VecInit(str.map(_.toByte.U(cfg.dataWidth.W)))
  val charCnt = RegInit(UInt(log2Up(str.length()).W), 0.U)

  sender.io.data.valid := true.B
  sender.io.data.bits := chars(charCnt)

  when(sender.io.data.ready) {
    when(charCnt === (chars.length - 1).U) {
      charCnt := 0.U
    } otherwise {
      charCnt := charCnt + 1.U
    }
  }

  io.ledR := sender.io.one
  io.ledG := sender.io.zero
  io.ledB := false.B
}

/**
 * Entry point that parses command-line arguments, creates a {@link HnyConfig} instance and emits
 * the corresponding SystemVerilog file for the {@link HNY2026} module.
 *
 * <pre>
 *   mill hny2026.runMain hny2026.HNY2026
 * </pre>
 *
 * Available arguments:
 * <ul>
 *   <li><b>clockFreq</b> - clock frequency in Hz (default 27 MHz)</li>
 * </ul>
 */
object HNY2026 extends App {
  val argsMap = args.map(
    _.split("=") match {
      case Array(name, value) => (name, value)
      case Array(name) => (name, "")
      case _ => ("", "")
    }).toMap

  val clockFreq = argsMap.getOrElse("clockFreq", "27000000").toInt
  println(s"Clock frequency = $clockFreq Hz")

  val cfg = HnyConfig(
    clockFreq = clockFreq,
    frameRate = 30,
    frameRateAccuracy = 0.0001,
    dataWidth = 8
  )

  ChiselStage.emitSystemVerilogFile(
    new HNY2026(cfg, "HNY2026! Vsem dobra :)")
  )
}
