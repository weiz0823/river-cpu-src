package model

import spinal.core._
import spinal.lib.IMasterSlave
import core.csr.CsrWrTypes
import spinal.lib.master
import core.csr.PrivilegeEnum

final case class CsrRWBundle() extends Bundle with IMasterSlave {
  val priv = PrivilegeEnum()
  val rdData = UInt(32 bits)
  val wrData = UInt(32 bits)
  // use CsrWrTypes.NONE for disable
  val wrType = CsrWrTypes()
  val instValid = Bool()

  override def asMaster() {
    out(priv, wrData, wrType)
    in(rdData, instValid)
  }

  def disable() {
    this.wrType := CsrWrTypes.NONE
  }

  def dummyDevice() {
    this.rdData := 0
    this.instValid := False
  }
}
