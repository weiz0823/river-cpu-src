package core

import spinal.core._
import model.BranchPredictBundle
import model.ExceptBundle

case class IFBundle() extends Bundle {
  val pc = UInt(32 bits)
  val inst = Bits(32 bits)
  val instFlush = Bool()
  val branchPredict = BranchPredictBundle()
  val except = ExceptBundle()
}

/** Register IF->ID stage.
  */
class IFRegs extends Component {
  val io = new Bundle {
    val iF = in(IFBundle())
    val id = out(Reg(IFBundle()))
    val ifStall, idStall = in(Bool())
  }

  io.id.instFlush.init(True)

  when(!io.idStall) {
    // should give something
    io.id := io.iF
    when(io.ifStall) {
      // insert bubble
      io.id.instFlush := True
    }
  }
  // otherwise(ID stall) keep everything

}
