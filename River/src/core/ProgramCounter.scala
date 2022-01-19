package core

import spinal.core._
import model._
import spinal.lib.PriorityMux

class ProgramCounter(pcInit: Long) extends Component {
  val io = new Bundle {
    val pc = out UInt (32 bits)
    val ifStall = in(Bool())
    val branch = in(BranchBundle())
    val trapBranch = in(BranchBundle())
    val branchPredict = in(BranchPredictBundle())
  }

  val regPc = RegInit(U(pcInit, 32 bits))
  io.pc := regPc
  val regBranch = Reg(BranchBundle())
  val regTrapBranch = Reg(BranchBundle())
  regBranch.e.init(False)
  regTrapBranch.e.init(False)
  when(io.ifStall & io.branch.e) {
    regBranch := io.branch
  }
  when(~io.ifStall) {
    regBranch.e := False
  }
  when(io.ifStall & io.trapBranch.e) {
    regTrapBranch := io.trapBranch
  }
  when(~io.ifStall) {
    regTrapBranch.e := False
  }

  when(!io.ifStall) {
    regPc := PriorityMux(
      Array(
        (regTrapBranch.e, regTrapBranch.addr),
        (io.trapBranch.e, io.trapBranch.addr),
        (regBranch.e, regBranch.addr),
        (io.branch.e, io.branch.addr),
        (io.branchPredict.take, io.branchPredict.addr),
        (True, regPc + 4)
      )
    )
  }
}
