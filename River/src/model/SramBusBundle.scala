package model

import spinal.core._
import spinal.lib.IMasterSlave

final case class SramBusBundle() extends Bundle with IMasterSlave {
  val wrData = UInt(32 bits)
  val rdData = UInt(32 bits)
  val addr = UInt(20 bits)
  val be = Bits(4 bits)
  val we = Bool()
  // cyc and stb merged to one
  val stb = Bool()
  val ack = Bool()

  override def asMaster(): Unit = {
    out(wrData, addr, be, we, stb)
    in(rdData, ack)
  }

  def disable() {
    stb := False
  }
}
