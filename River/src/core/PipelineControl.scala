package core

import spinal.core._
import spinal.lib.{IMasterSlave, master, slave}

final case class PipeStallBundle() extends Bundle with IMasterSlave {
  val req = Bool()
  val stall = Bool()

  override def asMaster(): Unit = {
    out(req)
    in(stall)
  }
}

/** Pipeline control. Combinational.
  */
class PipelineControl extends Component {
  val io = new Bundle {
    val iF = new Bundle {
      val pipeStall = slave(PipeStallBundle())
    }
    val id = new Bundle {
      val pipeStall = slave(PipeStallBundle())
    }
    val ex = new Bundle {
      val pipeStall = slave(PipeStallBundle())
    }
    val mem = new Bundle {
      val pipeStall = slave(PipeStallBundle())
    }
    val wb = new Bundle {
      val pipeStall = slave(PipeStallBundle())
    }
  }

  io.iF.pipeStall.stall := False
  io.id.pipeStall.stall := False
  io.ex.pipeStall.stall := False
  io.mem.pipeStall.stall := False
  io.wb.pipeStall.stall := False
  when(io.wb.pipeStall.req) {
    io.iF.pipeStall.stall := True
    io.id.pipeStall.stall := True
    io.ex.pipeStall.stall := True
    io.mem.pipeStall.stall := True
    io.wb.pipeStall.stall := True
  }.elsewhen(io.mem.pipeStall.req) {
    io.iF.pipeStall.stall := True
    io.id.pipeStall.stall := True
    io.ex.pipeStall.stall := True
    io.mem.pipeStall.stall := True
  }.elsewhen(io.ex.pipeStall.req) {
    io.iF.pipeStall.stall := True
    io.id.pipeStall.stall := True
    io.ex.pipeStall.stall := True
  }.elsewhen(io.id.pipeStall.req) {
    io.iF.pipeStall.stall := True
    io.id.pipeStall.stall := True
  }.elsewhen(io.iF.pipeStall.req) {
    io.iF.pipeStall.stall := True
  }
}
