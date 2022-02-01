package core.csr

import spinal.core._
import model._
import spinal.lib.{master, slave}
import config.CsrConfig

/** Control status register file. No decode csr content, but only read and write
  * it.
  *
  * Bacause csr write is not as direct as general registers, we put read and
  * write at same stage.
  */
class CsrFile(config: CsrConfig) extends Component {
  val io = new Bundle {
    val csrBus = slave(CsrBusBundle())
    val exStall = in(Bool())
    val exInstFlush = in(Bool())
    val privMode = in(PrivilegeEnum())

    val trapCsrs = master(TrapCsrBundle())
    val counterCsrs = master(CounterCsrBundle())
    val scratchCsr = master(CsrRWBundle())
    val atpCsr = master(CsrRWBundle())
  }

  val addrDecode = new CsrAddrDecode
  addrDecode.io.addr := io.csrBus.addr

  def defaultCsrConnection(csr: CsrRWBundle) {
    csr.wrType := CsrWrTypes.NONE
    csr.priv := addrDecode.io.priv
    csr.wrData := io.csrBus.wrData
  }

  defaultCsrConnection(io.trapCsrs.status)
  defaultCsrConnection(io.scratchCsr)
  defaultCsrConnection(io.trapCsrs.trapVec)
  defaultCsrConnection(io.trapCsrs.eDeleg)
  defaultCsrConnection(io.trapCsrs.iDeleg)
  defaultCsrConnection(io.trapCsrs.ip)
  defaultCsrConnection(io.trapCsrs.ie)
  defaultCsrConnection(io.trapCsrs.epc)
  defaultCsrConnection(io.trapCsrs.cause)
  defaultCsrConnection(io.trapCsrs.trapVal)
  defaultCsrConnection(io.counterCsrs.cycle)
  defaultCsrConnection(io.counterCsrs.cycleH)
  defaultCsrConnection(io.counterCsrs.time)
  defaultCsrConnection(io.counterCsrs.timeH)
  defaultCsrConnection(io.counterCsrs.instRet)
  defaultCsrConnection(io.counterCsrs.instRetH)
  defaultCsrConnection(io.atpCsr)

  // if set or clear no bits, then ignore write (mostly pure read instruction)
  val wrE = io.csrBus.wrType.mux(
    CsrWrTypes.NONE -> False,
    CsrWrTypes.WRITE -> True,
    CsrWrTypes.CLEAR -> (io.csrBus.wrData =/= 0),
    CsrWrTypes.SET -> (io.csrBus.wrData =/= 0)
  )
  // trapE=1 will invalidate current instruction in EX
  // exStall=1 should not perform any write
  val maskedWrType =
    Mux(io.exStall | io.exInstFlush, CsrWrTypes.NONE, io.csrBus.wrType)
  val csrExist = Bool()
  val csrWrOk = ~(wrE & addrDecode.io.readOnly)
  val csrPrivOk = io.privMode.asBits.asUInt >= addrDecode.io.priv.asBits.asUInt
  val notCsrInst = io.csrBus.wrType === CsrWrTypes.NONE
  io.csrBus.instValid := notCsrInst | (csrExist & csrWrOk & csrPrivOk)

  def connectCsr(csr: CsrRWBundle) {
    csrExist := csr.instValid
    io.csrBus.rdData := csr.rdData
    csr.wrType := maskedWrType
  }

  switch(addrDecode.io.basicAddr) {
    is(0x000) {
      // mstatus, sstatus
      connectCsr(io.trapCsrs.status)
    }
    is(0x001) {
      // misa: implement as hard wired
      io.csrBus.rdData := config.isa
      csrExist := addrDecode.io.priv === PrivilegeEnum.M
    }
    is(0x314) {
      // mhartid: hard-wired 0
      io.csrBus.rdData := 0
      csrExist := addrDecode.io.priv === PrivilegeEnum.M
    }
    is(0x002) {
      // medeleg
      connectCsr(io.trapCsrs.eDeleg)
    }
    is(0x003) {
      // mideleg
      connectCsr(io.trapCsrs.iDeleg)
    }
    is(0x004) {
      // mie, sie
      connectCsr(io.trapCsrs.ie)
    }
    is(0x005) {
      // mtvec, stvec
      connectCsr(io.trapCsrs.trapVec)
    }
    is(0x040) {
      // mscratch, sscratch
      connectCsr(io.scratchCsr)
    }
    is(0x041) {
      // mepc, sepc
      connectCsr(io.trapCsrs.epc)
    }
    is(0x042) {
      // mcause, scause
      connectCsr(io.trapCsrs.cause)
    }
    is(0x043) {
      // mtval, stval
      connectCsr(io.trapCsrs.trapVal)
    }
    is(0x044) {
      // mip, sip
      connectCsr(io.trapCsrs.ip)
    }
    is(0x300) {
      // cycle
      connectCsr(io.counterCsrs.cycle)
    }
    is(0x380) {
      // cycleh
      connectCsr(io.counterCsrs.cycleH)
    }
    is(0x301) {
      // time
      connectCsr(io.counterCsrs.time)
    }
    is(0x381) {
      // timeh
      connectCsr(io.counterCsrs.timeH)
    }
    is(0x302) {
      // instret
      connectCsr(io.counterCsrs.instRet)
    }
    is(0x382) {
      // instreth
      connectCsr(io.counterCsrs.instRetH)
    }
    is(M"00_1010_00--") {
      // pmpcfg0--3, not implemented
      io.csrBus.rdData := 0
      csrExist := addrDecode.io.priv === PrivilegeEnum.M
    }
    is(M"00_1011_----") {
      // pmpaddr0--15, not implemented
      io.csrBus.rdData := 0
      csrExist := addrDecode.io.priv === PrivilegeEnum.M
    }
    is(0x080) {
      // satp
      connectCsr(io.atpCsr)
    }
    default {
      io.csrBus.rdData := 0
      csrExist := False
    }
  }
}

object CsrWrTypes extends SpinalEnum {
  val NONE, WRITE, SET, CLEAR = newElement()
  defaultEncoding = SpinalEnumEncoding("staticEncoding")(
    NONE -> 0,
    WRITE -> 1,
    SET -> 2,
    CLEAR -> 3
  )
}

/** Privilege levels: machine, supervisor, hypervisor, user.
  */
object PrivilegeEnum extends SpinalEnum {
  val M, H, S, U = newElement()
  defaultEncoding = SpinalEnumEncoding("staticEncoding")(
    M -> 3,
    H -> 2,
    S -> 1,
    U -> 0
  )
}

class CsrAddrDecode extends Component {
  val io = new Bundle {
    val addr = in(UInt(12 bits))

    // privilege level
    val priv = out(PrivilegeEnum)
    val readOnly = out(Bool())
    val basicAddr = out(UInt(10 bits))
  }

  io.readOnly := io.addr(11 downto 10) === 3

  io.priv.assignFromBits(io.addr(9 downto 8).asBits)

  io.basicAddr := io.addr(11 downto 10) @@ io.addr(7 downto 0)
}
