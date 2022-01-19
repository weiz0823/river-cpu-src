package core

import spinal.core._
import model._

/** Branch decision in ID stage.
  */
class BranchDecide extends Component {
  val io = new Bundle {
    val inp = in(BranchInputBundle())
    val base = in(UInt(32 bits))
    val offset = in(UInt(32 bits))

    val branch = out(BranchBundle())
  }

  io.branch.addr := io.base + io.offset

  val result = io.inp.op.mux(
    BranchOp.EQ -> (io.inp.val1 === io.inp.val2),
    BranchOp.LT -> (io.inp.val1.asSInt < io.inp.val2.asSInt),
    BranchOp.LTU -> (io.inp.val1 < io.inp.val2),
    BranchOp.ALWAYS -> True
  )

  io.branch.e := result ^ io.inp.neg
}
