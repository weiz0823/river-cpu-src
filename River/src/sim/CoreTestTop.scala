package sim

import spinal.core._
import core._
import config.CoreConfig

class CoreTestTop extends Component {
  val io = new Bundle {
    val pc = out UInt (32 bits)
    val inst = in Bits (32 bits)
  }

  val coreConfig = CoreConfig()

  val pcComp = new ProgramCounter(coreConfig.pcInit)
  // instruction is given by stimulus
  val instFetch = new InstructionFetch
  val ifRegs = new IFRegs
  val instDecode = new InstructionDecode
  val regFile = new RegisterFile
  val idRegs = new IDRegs
  val instEx = new InstructionExecute
  val exRegs = new EXRegs
  val memRegs = new MEMRegs
  val pipeCtrl = new PipelineControl

  io.pc := pcComp.io.pc

  pcComp.io.ifStall := pipeCtrl.io.iF.pipeStall.stall

  instFetch.io.pc := pcComp.io.pc

  ifRegs.io.iF.inst := io.inst
  ifRegs.io.iF.pc := pcComp.io.pc
  ifRegs.io.ifStall := pipeCtrl.io.iF.pipeStall.stall
  ifRegs.io.idStall := pipeCtrl.io.id.pipeStall.stall

  instDecode.io.iF := ifRegs.io.id
  instDecode.io.regFileRd1 <> regFile.io.rd1
  instDecode.io.regFileRd2 <> regFile.io.rd2

  regFile.io.wr.data := memRegs.io.wb.regWb.data
  regFile.io.wr.addr := memRegs.io.wb.regWb.dest
  regFile.io.wr.e := memRegs.io.wb.regWb.e && memRegs.io.wb.regWb.valid
  // get input of stage registers as shortcut
  regFile.io.memWb := memRegs.io.mem.regWb
  regFile.io.exWb := exRegs.io.ex.regWb

  idRegs.io.id := instDecode.io.id
  idRegs.io.idStall := pipeCtrl.io.id.pipeStall.stall
  idRegs.io.exStall := pipeCtrl.io.ex.pipeStall.stall

  instEx.io.id := idRegs.io.ex

  exRegs.io.ex := instEx.io.ex
  exRegs.io.exStall := pipeCtrl.io.ex.pipeStall.stall
  exRegs.io.memStall := pipeCtrl.io.mem.pipeStall.stall

  memRegs.io.mem.regWb := exRegs.io.mem.regWb
  memRegs.io.mem.instFlush := exRegs.io.mem.instFlush
  memRegs.io.memStall := pipeCtrl.io.mem.pipeStall.stall
  memRegs.io.wbStall := pipeCtrl.io.wb.pipeStall.stall

  pipeCtrl.io.iF.pipeStall.req := False
  pipeCtrl.io.id.pipeStall.req := False
  pipeCtrl.io.ex.pipeStall.req := False
  pipeCtrl.io.mem.pipeStall.req := False
  pipeCtrl.io.wb.pipeStall.req := False
}
