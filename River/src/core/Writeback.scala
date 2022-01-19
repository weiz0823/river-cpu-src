package core

import spinal.core._
import model.RegisterWriteBundle

/** WB stage.
  */
class Writeback extends Component {
  val io = new Bundle {
    val mem = in(MEMBundle())
    val regWr = out(RegisterWriteBundle())
  }

  io.regWr.addr := io.mem.regWb.dest
  io.regWr.data := io.mem.regWb.data
  io.regWr.e := ~io.mem.instFlush && io.mem.regWb.e && io.mem.regWb.valid
}
