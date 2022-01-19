package model

import spinal.core._
import spinal.lib.IMasterSlave
import core.csr.CsrWrTypes

final case class CsrBusBundle() extends Bundle with IMasterSlave {
  val addr = UInt(12 bits)
  val wrData = UInt(32 bits)
  val wrType = CsrWrTypes()
  val rdData = UInt(32 bits)
  val instValid = Bool()

  override def asMaster() {
    out(addr, wrData, wrType)
    in(rdData, instValid)
  }
}
