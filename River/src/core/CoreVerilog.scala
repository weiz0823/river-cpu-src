package core

import spinal.core._
import scala.reflect.io.Directory
import peripheral._
import config.CoreConfig
import config.CsrConfig

object CoreSpinalConfig extends SpinalConfig {
  override val defaultConfigForClockDomains: ClockDomainConfig =
    ClockDomainConfig(resetKind = SYNC)
  override val targetDirectory: String = "generated_verilog/River"
  override val headerWithDate: Boolean = true
  // enum define problem
  // override val oneFilePerComponent: Boolean = true
}

object CoreVerilog {
  def main(args: Array[String]) {
    val dir = Directory(CoreSpinalConfig.targetDirectory)
    if (!dir.exists) dir.jfile.mkdirs()

    val coreTop = new CoreTop(CoreConfig(), CsrConfig())
    CoreSpinalConfig.generateVerilog(coreTop).printUnused()
    //.printPruned()
    //.printPrunedIo()
  }
}
