package core.trap

import spinal.core._
import model._
import spinal.lib.slave
import core.csr.PrivilegeEnum
import core.csr.CsrWriteControl
import spinal.lib.PriorityMux

class TrapTop extends Component {
  val io = new Bundle {
    val softInt = in(Bool())
    val timeInt = in(Bool())
    val extInt = in(Bool())
    val ex = new Bundle {
      val stall = in(Bool())
      val except = in(ExceptBundle())
      val trapRet = in(TrapRetBundle())
      val pc = in(UInt(32 bits))
      val instFlush = in(Bool())
    }
    val mem = new Bundle {
      val stall = in(Bool())
      val except = in(ExceptBundle())
      val pc = in(UInt(32 bits))
      val instFlush = in(Bool())
    }

    val trapBranch = out(BranchBundle())
    val privMode = out(PrivilegeEnum())
    val nextPrivMode = out(PrivilegeEnum())
    val flushEx = out(Bool())
    val flushMem = out(Bool())
    val exInstFlush = out(Bool())

    val csr = slave(TrapCsrBundle())
  }

  // components
  val statusWrCtrl = new CsrWriteControl
  val mvecWrCtrl = new CsrWriteControl
  val svecWrCtrl = new CsrWriteControl
  val midWrCtrl = new CsrWriteControl
  val medWrCtrl = new CsrWriteControl
  val mipWrCtrl = new CsrWriteControl
  val mieWrCtrl = new CsrWriteControl
  val mpcWrCtrl = new CsrWriteControl
  val spcWrCtrl = new CsrWriteControl
  val mcsWrCtrl = new CsrWriteControl
  val scsWrCtrl = new CsrWriteControl
  val mvalWrCtrl = new CsrWriteControl
  val svalWrCtrl = new CsrWriteControl

  // registers
  // begin: mstatus
  val regPrivMode = RegInit(PrivilegeEnum.M)
  val regPrevIntE = RegInit(B(0, 4 bits))
  val regGlobalIntE = RegInit(B(0, 4 bits))
  val regPrevPriv = spinal.core.Vec.fill(4)(RegInit(PrivilegeEnum.U))
  // MPRV, SUM, MXR
  val regMemPriv = RegInit(B"3'b110")
  // TVM, TW, TSR
  val regVirtSupport = RegInit(B"3'b000")
  // end: mstatus
  val mtvec = Reg(UInt(32 bits))
  val stvec = Reg(UInt(32 bits))
  val mideleg = RegInit(U(32 bits, default -> false))
  val medeleg = RegInit(U(32 bits, default -> false))
  // begin: mip
  val regSoftInt = RegNext(io.softInt)
  val regTimeInt = RegNext(io.timeInt)
  val regExtInt = RegNext(io.extInt)
  val regSSIP = RegInit(False)
  val regSTIP = RegInit(False)
  val regSEIP = RegInit(False)
  // end: mip
  val mie = Reg(UInt(32 bits))
  val mepc = Reg(UInt(32 bits))
  val sepc = Reg(UInt(32 bits))
  val mcause = RegInit(U(16, 32 bits))
  val scause = RegInit(U(16, 32 bits))
  val mtval = RegInit(U(0, 32 bits))
  val stval = RegInit(U(0, 32 bits))

  val regFlushEx = RegInit(False)
  val exInstFlush = io.ex.instFlush | regFlushEx

  // important values
  val intP = Bits(32 bits)
  // mask out hasInt when stall
  val hasInt = intP(11 downto 0).orR
  val intTaken =
    PriorityMux[UInt](
      intP(11 downto 0),
      Array.range(0, 12).map({ f => U(f, 5 bits) })
    )
  // only when pipeline not stalled, instruction is not bubble, can take trap
  val exTrapE =
    (hasInt | io.ex.except.e) & ~io.ex.stall & ~exInstFlush
  val memTrapE = io.mem.except.e & ~io.mem.stall & ~io.mem.instFlush
  val trapE = exTrapE | memTrapE
  val trapRetE = io.ex.trapRet.e & ~io.ex.stall & ~exInstFlush
  val trapCause = UInt(32 bits)
  trapCause(31) := hasInt // take interrupt if has interrupt
  trapCause(30 downto 0) := Mux(
    hasInt,
    intTaken.resize(31 bits),
    Mux(
      memTrapE,
      io.mem.except.code.asBits.asUInt.resize(31 bits),
      io.ex.except.code.asBits.asUInt.resize(31 bits)
    )
  )
  val trapVal = Mux(
    hasInt,
    U(0, 32 bits),
    Mux(memTrapE, io.mem.except.assistVal, io.ex.except.assistVal)
  )
  val trapPc = Mux(memTrapE, io.mem.pc, io.ex.pc)
  val mdeleg = Mux(trapCause(31), mideleg, medeleg)
  // support trap code 0~31
  val isDeleg = mdeleg(trapCause(4 downto 0))
  val trapPriv = Mux(isDeleg, PrivilegeEnum.S, PrivilegeEnum.M)
  val trapVec = Mux(isDeleg, stvec, mtvec)
  val mIntE = regPrivMode.mux(
    PrivilegeEnum.M -> regGlobalIntE(PrivilegeEnum.M.asBits.asUInt),
    default -> True
  )
  val sIntE = regPrivMode.mux(
    PrivilegeEnum.M -> False,
    PrivilegeEnum.S -> regGlobalIntE(PrivilegeEnum.S.asBits.asUInt),
    default -> True
  )

  // trap arbitration
  when(io.ex.stall & memTrapE) {
    // instruction in EX flushed because MEM takes trap
    regFlushEx := True
  }
  when(~io.ex.stall) {
    regFlushEx := False
  }
  // request to flush EX
  io.flushEx := trapE
  // request to flush MEM
  io.flushMem := memTrapE
  // provide EX flush information to CSR
  io.exInstFlush := exInstFlush

  // construct mstatus csr
  val mstatus = Bits(32 bits)
  mstatus(31) := False // SD
  mstatus(30 downto 23) := 0
  mstatus(22 downto 20) := regVirtSupport
  mstatus(19 downto 17) := regMemPriv
  mstatus(16 downto 13) := 0 // FS, XS
  mstatus(12 downto 11) := regPrevPriv(3).asBits // MPP
  mstatus(10 downto 9) := 0
  mstatus(8) := regPrevPriv(1).asBits(0) // SPP
  mstatus(7 downto 4) := regPrevIntE // xPIE
  mstatus(3 downto 0) := regGlobalIntE // xIE
  // warn: read/write ignore priviledge
  io.csr.status.rdData := mstatus.asUInt
  io.csr.status.instValid := (io.csr.status.priv === PrivilegeEnum.M) || (io.csr.status.priv === PrivilegeEnum.S)
  statusWrCtrl.io.current := mstatus.asUInt
  statusWrCtrl.connectCsrRW(io.csr.status)
  when(statusWrCtrl.io.e) {
    val next = statusWrCtrl.io.next.asBits
    regVirtSupport := next(22 downto 20)
    regMemPriv := next(19 downto 17)
    regPrevPriv(3).assignFromBits(next(12 downto 11))
    regPrevPriv(1) := Mux(next(8), PrivilegeEnum.S, PrivilegeEnum.U)
    regPrevIntE := next(7 downto 4)
    regGlobalIntE := next(3 downto 0)
  }

  // _tvec, m_deleg
  mvecWrCtrl.io.current := mtvec
  mvecWrCtrl.connectCsrRW(io.csr.trapVec)
  svecWrCtrl.io.current := stvec
  svecWrCtrl.connectCsrRW(io.csr.trapVec)
  midWrCtrl.io.current := mideleg
  midWrCtrl.connectCsrRW(io.csr.iDeleg)
  medWrCtrl.io.current := medeleg
  medWrCtrl.connectCsrRW(io.csr.eDeleg)
  io.csr.trapVec.rdData := io.csr.trapVec.priv.mux(
    PrivilegeEnum.M -> mtvec,
    default -> stvec
  )
  io.csr.trapVec.instValid := (io.csr.trapVec.priv === PrivilegeEnum.M) || (io.csr.trapVec.priv === PrivilegeEnum.S)
  when(mvecWrCtrl.io.e && io.csr.trapVec.priv === PrivilegeEnum.M) {
    mtvec := mvecWrCtrl.io.next
  }
  when(svecWrCtrl.io.e && io.csr.trapVec.priv === PrivilegeEnum.S) {
    stvec := svecWrCtrl.io.next
  }
  io.csr.iDeleg.rdData := mideleg
  io.csr.iDeleg.instValid := io.csr.iDeleg.priv === PrivilegeEnum.M
  when(midWrCtrl.io.e && io.csr.iDeleg.priv === PrivilegeEnum.M) {
    mideleg := midWrCtrl.io.next
  }
  io.csr.eDeleg.rdData := medeleg
  io.csr.eDeleg.instValid := io.csr.eDeleg.priv === PrivilegeEnum.M
  when(medWrCtrl.io.e && io.csr.eDeleg.priv === PrivilegeEnum.M) {
    medeleg := medWrCtrl.io.next
  }

  // construct mip
  val mip = B(
    32 bits,
    11 -> regExtInt, // MEIP
    9 -> regSEIP, // SEIP
    7 -> regTimeInt, // MTIP
    5 -> regSTIP, // STIP
    3 -> regSoftInt, // MSIP
    1 -> regSSIP, // SSIP
    default -> False
  )
  val maskedMIP = mip.asUInt | U(32 bits, 9 -> regExtInt, default -> False)
  mipWrCtrl.io.current := mip.asUInt
  mipWrCtrl.connectCsrRW(io.csr.ip)
  // warn: read/write ignore priviledge
  io.csr.ip.instValid := (io.csr.ip.priv === PrivilegeEnum.M) || (io.csr.ip.priv === PrivilegeEnum.S)
  io.csr.ip.rdData := maskedMIP
  when(mipWrCtrl.io.e) {
    val next = mipWrCtrl.io.next.asBits
    regSEIP := next(9)
    regSTIP := next(5)
    regSSIP := next(1)
  }
  // mie
  mieWrCtrl.io.current := mie
  mieWrCtrl.connectCsrRW(io.csr.ie)
  // warn: read/write ignore priviledge
  io.csr.ie.instValid := (io.csr.ie.priv === PrivilegeEnum.M) || (io.csr.ie.priv === PrivilegeEnum.S)
  io.csr.ie.rdData := mie
  when(mieWrCtrl.io.e) {
    mie := mieWrCtrl.io.next
  }
  for (i <- 0 to 31) {
    intP(i) := Mux(mideleg(i), sIntE, mIntE) & mie(i) & maskedMIP(i)
  }

  // _epc
  mpcWrCtrl.io.current := mepc
  mpcWrCtrl.connectCsrRW(io.csr.epc)
  when(mpcWrCtrl.io.e && io.csr.epc.priv === PrivilegeEnum.M) {
    mepc := mpcWrCtrl.io.next
  }
  spcWrCtrl.io.current := sepc
  spcWrCtrl.connectCsrRW(io.csr.epc)
  when(spcWrCtrl.io.e && io.csr.epc.priv === PrivilegeEnum.S) {
    sepc := spcWrCtrl.io.next
  }
  io.csr.epc.rdData := io.csr.epc.priv.mux(
    PrivilegeEnum.M -> mepc,
    default -> sepc
  )
  io.csr.epc.instValid := (io.csr.epc.priv === PrivilegeEnum.M) || (io.csr.epc.priv === PrivilegeEnum.S)
  // _cause
  mcsWrCtrl.io.current := mcause
  mcsWrCtrl.connectCsrRW(io.csr.cause)
  when(mcsWrCtrl.io.e && io.csr.cause.priv === PrivilegeEnum.M) {
    mcause := mcsWrCtrl.io.next
  }
  scsWrCtrl.io.current := scause
  scsWrCtrl.connectCsrRW(io.csr.cause)
  when(scsWrCtrl.io.e && io.csr.cause.priv === PrivilegeEnum.S) {
    scause := scsWrCtrl.io.next
  }
  io.csr.cause.rdData := io.csr.cause.priv.mux(
    PrivilegeEnum.M -> mcause,
    default -> scause
  )
  io.csr.cause.instValid := (io.csr.cause.priv === PrivilegeEnum.M) || (io.csr.cause.priv === PrivilegeEnum.S)
  // _tval
  mvalWrCtrl.io.current := mtval
  mvalWrCtrl.connectCsrRW(io.csr.trapVal)
  when(mvalWrCtrl.io.e && io.csr.trapVal.priv === PrivilegeEnum.M) {
    mtval := mvalWrCtrl.io.next
  }
  svalWrCtrl.io.current := stval
  svalWrCtrl.connectCsrRW(io.csr.trapVal)
  when(svalWrCtrl.io.e && io.csr.trapVal.priv === PrivilegeEnum.S) {
    stval := svalWrCtrl.io.next
  }
  io.csr.trapVal.rdData := io.csr.trapVal.priv.mux(
    PrivilegeEnum.M -> mtval,
    default -> stval
  )
  io.csr.trapVal.instValid := (io.csr.trapVal.priv === PrivilegeEnum.M) || (io.csr.trapVal.priv === PrivilegeEnum.S)

  // output
  io.privMode := regPrivMode
  io.trapBranch.e := trapE | trapRetE
  when(trapE) {
    io.trapBranch.addr := (trapVec(31 downto 2) + trapVec(1 downto 0).mux(
      0 -> 0,
      default -> trapCause(4 downto 0)
    )) @@ U(0, 2 bits)
  }.otherwise {
    io.trapBranch.addr := io.ex.trapRet.priv.mux(
      PrivilegeEnum.M -> mepc,
      default -> sepc
    )
  }

  // trap status transfer
  when(trapE) {
    val x = trapPriv.asBits.asUInt
    // trap to x, push stack
    regPrevIntE(x) := regGlobalIntE(x)
    regGlobalIntE(x) := False
    regPrevPriv(x) := regPrivMode
    regPrivMode := trapPriv
    io.nextPrivMode := trapPriv

    when(isDeleg) {
      sepc := trapPc
      scause := trapCause
      stval := trapVal
    }.otherwise {
      mepc := trapPc
      mcause := trapCause
      mtval := trapVal
    }
  }.elsewhen(trapRetE) {
    val x = io.ex.trapRet.priv.asBits.asUInt
    // xret, pop stack
    regGlobalIntE(x) := regPrevIntE(x)
    regPrevIntE(x) := True
    regPrivMode := regPrevPriv(x)
    io.nextPrivMode := regPrevPriv(x)
    regPrevPriv(x) := PrivilegeEnum.U
  } otherwise {
    io.nextPrivMode := regPrivMode
  }
}
