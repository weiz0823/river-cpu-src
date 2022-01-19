package model

import spinal.core._

object BranchOp extends SpinalEnum {
  val EQ, LT, LTU, ALWAYS = newElement()
  defaultEncoding = SpinalEnumEncoding("staticEncoding")(
    EQ -> 0,
    LT -> 2,
    LTU -> 3,
    ALWAYS -> 1
  )
}

case class BranchBundle() extends Bundle {
  val e = Bool()
  val addr = UInt(32 bits)
}

final case class BranchInputBundle() extends Bundle {
  val op = BranchOp()
  val neg = Bool()
  val val1 = UInt(32 bits)
  val val2 = UInt(32 bits)
}
