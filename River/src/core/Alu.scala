package core

import spinal.core._
import spinal.lib._
import model._

/** ALU. Combinational.
  */
class Alu extends Component {
  val io = new Bundle {
    // EX stage
    val inp = in(AluInputBundle())
    val result = out UInt (32 bits)
  }

  switch(io.inp.sel) {
    is(AluSel.ALU) {
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
        is(AluOp.ANDN) {
          io.result := (io.inp.val1 & ~io.inp.val2)
        }
        default {
          // AluOp.ADD
          io.result := io.inp.val1 + io.inp.val2
        }
      }
    }
    is(AluSel.SBU) {
      switch(io.inp.op.asBits) {
        is(B"4'b0101") {
          io.result := io.inp.val1 | (U(1, 32 bits) |<< (io.inp.val2 & 31)(4 downto 0))
        }
        default {
          // default to val1, why
          io.result := io.inp.val1
        }
      }
    }
    is(AluSel.BCNTU) {
      switch(io.inp.op.asBits) {
        is(B"4'b0010") {
          val val1 = io.inp.val1
          io.result := Vec.tabulate(32)(val1(_).asUInt(32 bits)).reduce(_ + _)
        }
        default {
          io.result := io.inp.val1
        }
      }
    }
  }
}
