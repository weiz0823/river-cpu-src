package model

import spinal.core._
import spinal.lib.IMasterSlave

/** Read/write memory with 32-bit physical address.
  */
final case class InternalBusBundle() extends Bundle with IMasterSlave {
  val wrData = UInt(32 bits)
  val rdData = UInt(32 bits)
  val addr = UInt(32 bits)
  val be = Bits(4 bits)
  val we = Bool()
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
