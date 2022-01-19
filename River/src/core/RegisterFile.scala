package core

import spinal.core._
import spinal.lib.slave
import model._

/** Register file with one input port and two output ports.
  *
  * If you read and write same register, you will get new value.
  */
class RegisterFile extends Component {
  val io = new Bundle {
    val rd1 = slave(RegisterReadBundle())
    val rd2 = slave(RegisterReadBundle())
    val exWb = in(RegWriteBackBundle())
    val memWb = in(RegWriteBackBundle())
    val wr = slave(RegisterWriteBundle())
    val stallReq = out(Bool())
    val wbStall = in(Bool())
  }

  val regFile = spinal.core.Vec.fill(32)(RegInit(U(0, 32 bits)))

  val rd1Shortcut = new RegReadShortcut
  val rd2Shortcut = new RegReadShortcut

  rd1Shortcut.io.rdAddr := io.rd1.addr
  rd1Shortcut.io.exWb := io.exWb
  rd1Shortcut.io.memWb := io.memWb
  rd1Shortcut.io.wr <> io.wr

  rd2Shortcut.io.rdAddr := io.rd2.addr
  rd2Shortcut.io.exWb := io.exWb
  rd2Shortcut.io.memWb := io.memWb
  rd2Shortcut.io.wr <> io.wr

  io.stallReq := False

  // == combinational ==
  when(io.rd1.addr =/= 0 & io.rd1.e & rd1Shortcut.io.stallReq) {
    io.stallReq := True
  }
  when(io.rd1.addr === 0) {
    io.rd1.data := 0
  }.elsewhen(rd1Shortcut.io.valid) {
    // shortcut
    io.rd1.data := rd1Shortcut.io.data
  }.otherwise {
    io.rd1.data := regFile(io.rd1.addr)
  }

  // same as above for rd2
  when(io.rd2.addr =/= 0 & io.rd2.e & rd2Shortcut.io.stallReq) {
    io.stallReq := True
  }
  when(io.rd2.addr === 0) {
    io.rd2.data := 0
  }.elsewhen(rd2Shortcut.io.valid) {
    // shortcut
    io.rd2.data := rd2Shortcut.io.data
  }.otherwise {
    io.rd2.data := regFile(io.rd2.addr)
  }

  // == sequential ==

  // discard write to x0, which is always 0
  when(!io.wbStall && io.wr.e && io.wr.addr =/= 0) {
    regFile(io.wr.addr) := io.wr.data
  }
}

/** Combine writeback requests to get a shortcut data.
  */
class RegReadShortcut extends Component {
  val io = new Bundle {
    val exWb = in(RegWriteBackBundle())
    val memWb = in(RegWriteBackBundle())
    val wr = slave(RegisterWriteBundle())

    val rdAddr = in UInt (5 bits)
    val data = out UInt (32 bits)
    val valid = out(Bool())
    val stallReq = out(Bool())
  }

  io.stallReq := False
  io.valid := False
  io.data := io.exWb.data

  when(io.exWb.e && io.exWb.dest === io.rdAddr) {
    when(io.exWb.valid) {
      // shortcut from EX
      io.data := io.exWb.data
      io.valid := True
    }.otherwise {
      // EX should write back, but data is not valid yet
      io.stallReq := True
    }
  }.elsewhen(io.memWb.e && io.memWb.dest === io.rdAddr) {
    when(io.memWb.valid) {
      // shortcut from MEM
      io.data := io.memWb.data
      io.valid := True
    }.otherwise {
      // MEM should write back, but data is not valid yet
      io.stallReq := True
    }
  }.elsewhen(io.wr.e && io.wr.addr === io.rdAddr) {
    // shortcut from WB
    io.data := io.wr.data
    io.valid := True
  }
}
