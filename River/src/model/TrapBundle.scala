package model

import spinal.core._
import core.csr.PrivilegeEnum

final case class TrapBundle() extends Bundle {
  val e = Bool()
  val isRet = Bool()
  val priv = PrivilegeEnum()
}
