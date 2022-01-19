package model

import spinal.core._
import spinal.lib.IMasterSlave

final case class CLINTBusBundle() extends Bundle with IMasterSlave {
  val addr = UInt(14 bits)
  val wrData = UInt(32 bits)
  val rdData = UInt(32 bits)
  val we = Bool()
  val be = Bits(4 bits)
  val stb = Bool()
  val ack = Bool()

  override def asMaster(): Unit = {
    out(stb, we, be, addr, wrData)
    in(ack, rdData)
  }

  def disable() {
    stb := False
  }
}
