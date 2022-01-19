package core

import spinal.core._
import model._

case class MEMBundle() extends Bundle {
  val regWb = RegWriteBackBundle()
  val instFlush = Bool()
}

/** Registers MEM->WB stage.
  */
class MEMRegs extends Component {
  val io = new Bundle {
    val mem = in(MEMBundle())
    val wb = out(Reg(MEMBundle()))
    val memStall = in(Bool())
    val wbStall = in(Bool())
  }

  io.wb.instFlush.init(True)

  when(!io.wbStall) {
    // should give something
    io.wb := io.mem
    when(io.memStall) {
      // insert bubble
      io.wb.regWb.disable()
      io.wb.instFlush := True
    }
  }
}
