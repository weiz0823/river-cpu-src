package core

import spinal.core._
import model._

/** ALU. Combinational.
  */
class Alu extends Component {
  val io = new Bundle {
    // EX stage
    val inp = in(AluInputBundle())
    val result = out UInt (32 bits)
  }

  switch(io.inp.op) {
    // AluOp.ADD is default
    is(AluOp.SUB) {
      io.result := io.inp.val1 - io.inp.val2
    }
    is(AluOp.AND) {
      io.result := io.inp.val1 & io.inp.val2
    }
    is(AluOp.OR) {
      io.result := io.inp.val1 | io.inp.val2
    }
    is(AluOp.XOR) {
      io.result := io.inp.val1 ^ io.inp.val2
    }
    is(AluOp.SLL) {
      io.result := io.inp.val1 |<< io.inp.val2(4 downto 0)
    }
    is(AluOp.SRL) {
      io.result := io.inp.val1 |>> io.inp.val2(4 downto 0)
    }
    is(AluOp.SRA) {
      io.result := (io.inp.val1.asSInt >> io.inp.val2(4 downto 0)).asUInt
    }
    is(AluOp.SLT) {
      io.result := (io.inp.val1.asSInt < io.inp.val2.asSInt).asUInt.resized
    }
    is(AluOp.SLTU) {
      io.result := (io.inp.val1 < io.inp.val2).asUInt.resized
    }
    default {
      // AluOp.ADD
      io.result := io.inp.val1 + io.inp.val2
    }
  }
}
