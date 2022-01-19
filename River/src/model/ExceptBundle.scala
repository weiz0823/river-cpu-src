package model

import spinal.core._
import core.csr.PrivilegeEnum

final case class ExceptBundle() extends Bundle {
  val e = Bool()
  val code = ExceptCode()
  // check privileged spec 3.1.17 for what should be set for _tval
  val assistVal = UInt(32 bits)

  def makeIllegalInstruction(inst: Bits) {
    e := True
    code := ExceptCode.ILLEGAL_INST
    assistVal := inst.asUInt
  }
}

final case class TrapRetBundle() extends Bundle {
  val e = Bool()
  val priv = PrivilegeEnum()
}
