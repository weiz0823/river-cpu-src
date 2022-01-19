package model

import spinal.core._
import spinal.lib.IMasterSlave

case class UartControlBundle() extends Bundle with IMasterSlave {
  val ren, wen = Bool()
  val ready, tbre, tsre = Bool()
  val wrData, rdData = UInt(8 bits)

  override def asMaster(): Unit = {
    out(ren, wen, wrData)
    in(ready, tbre, tsre, rdData)
  }

  def disable() {
    ren := True
    wen := True
  }
}
