package core

import spinal.core._
import spinal.lib.master
import model.InternalBusBundle

/** MEM stage.
  */
class MemoryAccess extends Component {
  val io = new Bundle {
    val ex = in(EXBundle())
    val mem = out(MEMBundle())
    val stallReq = out(Bool())
    val dataBus = master(InternalBusBundle())
  }

  val memRWComp = new MemRW
  memRWComp.io.dataBus <> io.dataBus
  io.stallReq := memRWComp.io.stallReq

  io.mem.instFlush := io.ex.instFlush
  memRWComp.io.memRw := io.ex.memRw
  io.mem.regWb := io.ex.regWb
  when(io.ex.instFlush) {
    // mask out flushed instruction
    io.mem.regWb.disable()
    memRWComp.io.memRw.disable()
  }

  when(io.ex.memRw.ce && ~io.ex.memRw.we) {
    io.mem.regWb.data := memRWComp.io.memRdData
    io.mem.regWb.valid := ~memRWComp.io.stallReq
  }
}
