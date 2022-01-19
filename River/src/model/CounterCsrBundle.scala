package model

import spinal.core._
import spinal.lib.IMasterSlave
import spinal.lib.master

final case class CounterCsrBundle() extends Bundle with IMasterSlave {
  val cycle, cycleH = CsrRWBundle()
  val time, timeH = CsrRWBundle()
  val instRet, instRetH = CsrRWBundle()

  override def asMaster(): Unit = {
    master(cycle, cycleH, time, timeH, instRet, instRetH)
  }
}
