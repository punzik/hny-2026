package hny2026

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

/**
 * Run: mill hny2026.runMain hny2026.TestGen_StrobeGenerator
 */
object TestGen_StrobeGenerator extends App {
  println(ChiselStage.emitSystemVerilog(
    new StrobeGenerator(HnyConfig(27000000, 30.3)),
    firtoolOpts = Array(
      "--disable-all-randomization",
      "--strip-debug-info"
    )
  ))
}

/**
 * Run: mill hny2026.runMain hny2026.TestGen_CharSender
 */
object TestGen_CharSender extends App {
  println(ChiselStage.emitSystemVerilog(
    new CharSender(HnyConfig(27000000, 30.3)),
    firtoolOpts = Array(
      "--disable-all-randomization",
      "--strip-debug-info"
    )
  ))
}

/**
 * Run: mill hny2026.runMain hny2026.TestGen_HNY2026
 */
object TestGen_HNY2026 extends App {
  println(ChiselStage.emitSystemVerilog(
    new HNY2026(HnyConfig(27000000, 30.3), "Hello!"),
    firtoolOpts = Array(
      "--disable-all-randomization",
      "--strip-debug-info"
    )
  ))
}
