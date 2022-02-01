package model

import spinal.core._

final case class TLBEntry() extends Bundle {
  val pte = InternalPTE()
  val vpn = UInt(20 bits)
  val valid = Bool()

  def hit(reqVpn: UInt) = (valid & reqVpn === vpn)
}
