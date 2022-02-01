package model

import spinal.core._

case class InternalPTE() extends Bundle {
  val ppn = UInt(22 bits)
  val R = Bool()
  val W = Bool()
  val X = Bool()
  val U = Bool()
  val G = Bool()
  // bit V, A, D and field RSW omitted

  def parseFromUInt(data: UInt) {
    ppn := data(31 downto 10)
    G := data(5)
    U := data(4)
    X := data(3)
    W := data(2)
    R := data(1)
  }
}

case class PTE() extends Bundle {
  val V = Bool()
  val R = Bool()
  val W = Bool()
  val X = Bool()
  val U = Bool()
  val G = Bool()
  val A = Bool()
  val D = Bool()
  val rsw = Bits(2 bit)
  val ppn = UInt(22 bits)
}
