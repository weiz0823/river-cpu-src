package model

import spinal.core._

object AluCalcType extends SpinalEnum {
  val DISABLE = newElement();
  val ARITH, ADDR, BRANCH = newElement();
}

object AluOp extends SpinalEnum {
  val ADD, SLL, SLT, SLTU, XOR, SRL, OR, AND, SUB, SRA, ANDN = newElement();
  defaultEncoding = SpinalEnumEncoding("staticEncoding")(
    // ALU-OP
    ADD -> 0,
    SLL -> 1,
    SLT -> 2,
    SLTU -> 3,
    XOR -> 4,
    SRL -> 5,
    OR -> 6,
    AND -> 7,
    SUB -> 8,
    SRA -> 13,
    ANDN -> 15,
  )
}

object AluSel extends SpinalEnum {
  val ALU, SBU, BCNTU = newElement()
}

final case class AluInputBundle() extends Bundle {
  val op = AluOp()
  val sel = AluSel() // select which calculate unit to operate
  val val1 = UInt(32 bits)
  val val2 = UInt(32 bits)
}
