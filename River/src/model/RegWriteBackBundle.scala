package model

import spinal.core._

final case class RegWriteBackBundle() extends Bundle {
  val dest = UInt(5 bits)
  val e = Bool()
  val data = UInt(32 bits)
  val valid = Bool()

  def disable() {
    e := False
  }
}
