package hny2026

import chisel3._
import chisel3.util._

/**
 * Configuration container for the HNY2026 design.
 *
 * <p>The parameters control the timing of the strobe generator and the serial transmission of
 * characters to the LED matrix.</p>
 *
 * @param clockFreq          The frequency of the input clock in hertz.
 *                           Default is 27 MHz.
 * @param frameRate          Desired frame rate in frames-per-second.
 *                           The design uses this to calculate the
 *                           strobe period; e.g. 30 fps is the
 *                           default for my phone camera.
 * @param frameRateAccuracy  The acceptable relative error on the frame
 *                           rate. A smaller value yields a more
 *                           accurate strobe but may increase the
 *                           hardware resource usage.  The default
 *                           (0.0001) gives <0.01 % error.
 * @param dataWidth          Width in bits of the data payload for each
 *                           character. The default is 8 bits, matching
 *                           an ASCII byte. The actual transmitted
 *                           stream is `dataWidth + 2` bits, the
 *                           additional bits being a parity bit and a
 *                           empty ending bit.
 */
case class HnyConfig(
  clockFreq: Int = 27000000,
  frameRate: Double = 30.0,
  frameRateAccuracy: Double = 0.0001,
  dataWidth: Int = 8
)
