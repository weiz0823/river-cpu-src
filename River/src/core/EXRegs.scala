package core

import spinal.core._
import model._

case class EXBundle() extends Bundle {
  val regWb = RegWriteBackBundle()
  val memRw = MemRWBundle()
  val instFlush = Bool()
}

/** Registers EX->MEM stage.
  */
class EXRegs extends Component {
  val io = new Bundle {
    val ex = in(EXBundle())
    val mem = out(Reg(EXBundle()))
    val exStall = in(Bool())
    val memStall = in(Bool())
  }

  io.mem.instFlush.init(True)

  when(!io.memStall) {
    // should give something
    io.mem := io.ex
    when(io.exStall) {
      // insert bubble
      io.mem.regWb.disable()
      io.mem.memRw.disable()
      io.mem.instFlush := True
    }
  }
}
