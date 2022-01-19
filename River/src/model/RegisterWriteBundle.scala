package model

import spinal.core._
import spinal.lib.IMasterSlave

case class RegisterWriteBundle(addrBits: BitCount = 5 bits)
    extends Bundle
    with IMasterSlave {
  val e = Bool()
  val addr = UInt(addrBits)
  val data = UInt(32 bits)

  def asMaster(): Unit = {
    out(addr, e, data)
  }

  def disable() {
    e := False
  }
}
