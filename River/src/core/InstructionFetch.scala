package core

import spinal.core._
import spinal.lib.master
import model._

/** Instruction fetch.
  *
  * Currently instruction will be fetched over and over again if stalled.
  */
class InstructionFetch extends Component {
  val io = new Bundle {
    val pc = in UInt (32 bits)
    // if not enabled, give flushed instruction
    val e = in(Bool())

    val iF = out(IFBundle())
    val stallReq = out(Bool())
    val instBus = master(InternalBusBundle())
    val ifStall = in(Bool())
    val flushCurrent = in(Bool())
    val branchPredict = out(BranchPredictBundle())
    val mmuExcept = in(ExceptBundle())
  }

  val regInst = RegNextWhen(io.instBus.rdData, io.instBus.ack)
  val regExcept = Reg(ExceptBundle())
  regExcept.e.init(False)
  val regInstValid = RegInit(False)
  when(io.instBus.ack & io.ifStall) {
    regInstValid := True
    regExcept := io.mmuExcept
  }
  when(~io.ifStall) {
    regInstValid := False
    regExcept.e := False
  }
  val inst = Mux(regInstValid, regInst, io.instBus.rdData).asBits

  val predictComp = new BranchPredict
  predictComp.io.inst := inst
  predictComp.io.pc := io.pc
  io.branchPredict.addr := predictComp.io.predict.addr
  io.branchPredict.take := ~io.ifStall & io.e & predictComp.io.predict.take

  // default values
  io.instBus.stb := io.e & ~regInstValid
  io.instBus.we := False
  io.instBus.wrData := 0
  io.instBus.be := B"4'b1111"
  io.instBus.addr := io.pc

  io.iF.pc := io.pc
  io.iF.inst := inst
  io.iF.branchPredict := predictComp.io.predict
  io.iF.except := Mux(regExcept.e, regExcept, io.mmuExcept)

  io.stallReq := io.e & ~regInstValid & ~io.instBus.ack

  // registers
  val regFlushCurrent = RegInit(False)
  when(io.ifStall & io.flushCurrent) {
    regFlushCurrent := True
  }
  when(~io.ifStall) {
    regFlushCurrent := False
  }
  io.iF.instFlush := ~io.e | io.flushCurrent | regFlushCurrent
}
