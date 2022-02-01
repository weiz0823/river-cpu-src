package top

import scala.reflect.io.Directory
import spinal.core._

import core.mmu._

object TopSpinalConfig extends SpinalConfig {
  override val defaultConfigForClockDomains: ClockDomainConfig =
    ClockDomainConfig(resetKind = ASYNC)
  override val targetDirectory: String = "generated_verilog/River"
  override val headerWithDate: Boolean = true
  // enum define problem
  // override val oneFilePerComponent: Boolean = true
}

object TopVerilog {
  def main(args: Array[String]) {
    val dir = Directory(TopSpinalConfig.targetDirectory)
    if (!dir.exists) dir.jfile.mkdirs()

    TopSpinalConfig.generateVerilog(new thinpad_top).printUnused()
    //.printPruned()
    //.printPrunedIo()
  }
}
