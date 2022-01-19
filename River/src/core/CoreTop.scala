package core

import spinal.core._
import spinal.lib.master
import model.InternalBusBundle
import config.CoreConfig
import config.CsrConfig
import model.ExceptCode

class CoreTop(config: CoreConfig, csrConfig: CsrConfig) extends Component {
  val io = new Bundle {
    val instBus = master(InternalBusBundle())
    val dataBus = master(InternalBusBundle())
    val softInt = in(Bool())
    val timeInt = in(Bool())
    val extInt = in(Bool())
    val timeCsrData = in(UInt(64 bits))
  }

  val pcComp = new ProgramCounter(config.pcInit)
  val instFetch = new InstructionFetch
  val ifRegs = new IFRegs
  val instDecode = new InstructionDecode
  val regFile = new RegisterFile
  val idRegs = new IDRegs
  val instEx = new InstructionExecute
  val csrFile = new csr.CsrFile(csrConfig)
  val exRegs = new EXRegs
  val memAccess = new MemoryAccess
  val memRegs = new MEMRegs
  val wbComp = new Writeback
  val pipeCtrl = new PipelineControl

  val scratchCsrComp = new csr.ScratchCsr
  val counterComp = new csr.Counters
  val trapTop = new trap.TrapTop

  pcComp.io.ifStall := pipeCtrl.io.iF.pipeStall.stall
  pcComp.io.branch := instDecode.io.branch
  pcComp.io.trapBranch := trapTop.io.trapBranch
  pcComp.io.branchPredict := instFetch.io.branchPredict

  instFetch.io.pc := pcComp.io.pc
  instFetch.io.e := ~clockDomain.isResetActive
  instFetch.io.instBus <> io.instBus
  instFetch.io.ifStall := pipeCtrl.io.iF.pipeStall.stall
  // invalidate instruction in IF
  instFetch.io.flushCurrent := instDecode.io.branch.e | trapTop.io.trapBranch.e
  instFetch.io.mmuExcept.e := False
  instFetch.io.mmuExcept.code := ExceptCode.INST_PAGE
  instFetch.io.mmuExcept.assistVal := pcComp.io.pc

  ifRegs.io.iF := instFetch.io.iF
  ifRegs.io.ifStall := pipeCtrl.io.iF.pipeStall.stall
  ifRegs.io.idStall := pipeCtrl.io.id.pipeStall.stall

  instDecode.io.iF := ifRegs.io.id
  instDecode.io.regFileRd1 <> regFile.io.rd1
  instDecode.io.regFileRd2 <> regFile.io.rd2
  instDecode.io.idStall := pipeCtrl.io.id.pipeStall.stall
  // invalidate instruction in ID
  instDecode.io.flushCurrent := trapTop.io.trapBranch.e

  regFile.io.wr := wbComp.io.regWr
  // get input of stage registers as shortcut
  regFile.io.memWb := memRegs.io.mem.regWb
  regFile.io.exWb := exRegs.io.ex.regWb
  regFile.io.wbStall := pipeCtrl.io.wb.pipeStall.stall

  idRegs.io.id := instDecode.io.id
  idRegs.io.idStall := pipeCtrl.io.id.pipeStall.stall
  idRegs.io.exStall := pipeCtrl.io.ex.pipeStall.stall

  instEx.io.id := idRegs.io.ex
  instEx.io.exStall := pipeCtrl.io.ex.pipeStall.stall
  // when any exception or interrupt happens, invalidate instruction in EX
  instEx.io.flushCurrent := trapTop.io.trapE
  instEx.io.privMode := trapTop.io.privMode

  csrFile.io.csrBus <> instEx.io.csrBus
  csrFile.io.scratchCsr <> scratchCsrComp.io.scratchCsr
  csrFile.io.trapCsrs <> trapTop.io.csr
  csrFile.io.counterCsrs <> counterComp.io.csr
  csrFile.io.exStall := pipeCtrl.io.ex.pipeStall.stall
  csrFile.io.trapE := trapTop.io.trapE
  csrFile.io.privMode := trapTop.io.privMode

  exRegs.io.ex := instEx.io.ex
  exRegs.io.exStall := pipeCtrl.io.ex.pipeStall.stall
  exRegs.io.memStall := pipeCtrl.io.mem.pipeStall.stall

  memAccess.io.ex := exRegs.io.mem
  memAccess.io.dataBus <> io.dataBus

  memRegs.io.mem := memAccess.io.mem
  memRegs.io.memStall := pipeCtrl.io.mem.pipeStall.stall
  memRegs.io.wbStall := pipeCtrl.io.wb.pipeStall.stall

  wbComp.io.mem := memRegs.io.wb

  pipeCtrl.io.iF.pipeStall.req := instFetch.io.stallReq
  pipeCtrl.io.id.pipeStall.req := regFile.io.stallReq
  pipeCtrl.io.ex.pipeStall.req := False
  pipeCtrl.io.mem.pipeStall.req := memAccess.io.stallReq
  pipeCtrl.io.wb.pipeStall.req := False

  counterComp.io.timeData := io.timeCsrData
  counterComp.io.currentInstValid := ~idRegs.io.ex.instFlush

  trapTop.io.exStall := pipeCtrl.io.ex.pipeStall.stall
  trapTop.io.except := instEx.io.except
  trapTop.io.trapRet := instEx.io.trapRet
  trapTop.io.pc := idRegs.io.ex.pc
  trapTop.io.softInt := io.softInt
  trapTop.io.timeInt := io.timeInt
  trapTop.io.extInt := io.extInt
  trapTop.io.instFlush := idRegs.io.ex.instFlush
}
