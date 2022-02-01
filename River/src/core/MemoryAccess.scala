package core

import spinal.core._
import spinal.lib.master
import model.InternalBusBundle
import model.ExceptBundle
import core.mmu.TLBOp
import model.ExceptCode

/** MEM stage.
  */
class MemoryAccess extends Component {
  val io = new Bundle {
    val ex = in(EXBundle())
    val mem = out(MEMBundle())

    val flushCurrent = in(Bool())
    val memStall = in(Bool())
    val mmuExcept = in(ExceptBundle())
    val except = out(ExceptBundle())
    val stallReq = out(Bool())
    val tlbReqOp = out(TLBOp())
    val dataBus = master(InternalBusBundle())
  }

  val memRWComp = new MemRW
  memRWComp.io.dataBus <> io.dataBus
  io.stallReq := memRWComp.io.stallReq

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

  val except = ExceptBundle()
  except.e := io.ex.memRw.ce & memRWComp.io.misalign
  except.code := Mux(
    io.ex.memRw.we,
    ExceptCode.STORE_MISALIGN,
    ExceptCode.LOAD_MISALIGN
  )
  except.assistVal := io.ex.memRw.addr

  io.except := Mux(except.e, except, io.mmuExcept)

  // registers
  val regFlushCurrent = RegInit(False)
  when(io.memStall & io.flushCurrent) {
    regFlushCurrent := True
  }
  when(~io.memStall) {
    regFlushCurrent := False
  }
  io.mem.instFlush := io.ex.instFlush | io.flushCurrent | regFlushCurrent

  val regTlbReqDone = RegNext(io.memStall)
  io.tlbReqOp := Mux(regTlbReqDone, TLBOp.NOP, io.ex.tlbReqOp)
}
