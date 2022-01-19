package model

import spinal.core._
import spinal.lib.IMasterSlave

// always master
case class SramControlBundle() extends Bundle with IMasterSlave {
  val wrData = UInt(32 bits)
  val rdData = UInt(32 bits)
  val addr = UInt(20 bits)
  val ben = Bits(4 bits)
  val cen, oen, wen = Bool()

  override def asMaster(): Unit = {
    out(wrData, addr, ben, cen, oen, wen)
    in(rdData)
  }

  def setMasterDisabled() {
    wrData := 0
    addr := 0
    ben := B"4'b1111"
    wen := True
    cen := True
    oen := True
  }
}
