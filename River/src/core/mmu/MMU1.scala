package core.mmu

import spinal.core._
import model._
import spinal.lib.{master, slave}
import core.csr.PrivilegeEnum
import config.MMU1Config

/** MMU for 1 stage.
  */
class MMU1(config: MMU1Config) extends Component {
  val io = new Bundle {
    val bus = master(InternalBusBundle())
    val req = slave(InternalBusBundle())
    val except = out(ExceptBundle())

    val tlbReqOp = in(TLBOp())

    //// from CSRs
    val satp = in(UInt(32 bits))
    val priv = in(PrivilegeEnum())
    val mxr = in(Bool())
    val sum = in(Bool())
  }

  // could be configured
  val tlb = new TLB(4, 2)
  val ptw = new PageTableWalker

  val enable = io.satp(31) && io.priv =/= PrivilegeEnum.M
  val vpn = io.req.addr(31 downto 12)
  val offset = io.req.addr(11 downto 0)
  val tlbRes = TLBQueryResult()
  tlb.io.query.vpn := vpn
  tlbRes.hit := tlb.io.query.hit
  tlbRes.pte := tlb.io.query.pte

  tlb.io.req := ptw.io.tlbReq
  when(io.tlbReqOp =/= TLBOp.NOP) {
    tlb.io.req.op := io.tlbReqOp
  }
  val ptwFault = ptw.io.ack && ptw.io.fault
  ptw.io.e := enable && io.req.stb && !tlbRes.hit
  ptw.io.vpn := vpn
  ptw.io.basePPN := io.satp(21 downto 0)
  ptw.io.abort := False

  // physical address register
  val regTlbAddr = Reg(UInt(34 bits))
  val regTlbHit = RegInit(False)
  val physAddr = Mux(enable, regTlbAddr, io.req.addr)
  val addrValid = ~enable | regTlbHit

  val addrMisalignCheck = new Area {
    // TODO
  }

  // user check area
  val isUser = (io.priv === PrivilegeEnum.U)
  val userCheckOk = if (config.isInst) {
    !(tlbRes.pte.U ^ isUser)
  } else {
    Mux(tlbRes.pte.U, io.sum || isUser, !isUser)
  }

  // page fault
  val pageFault = ~userCheckOk |
    (if (config.isInst) {
       !tlbRes.pte.X
     } else {
       // basic V & R bit check is already handled by PTW (R || X)
       val readPermOk =
         tlbRes.pte.R || io.mxr // X && (Make eXecutable Readable)
       val writePermOk = !io.req.we || tlbRes.pte.W
       val rwOk = readPermOk && writePermOk
       ~rwOk
     })

  // exception generation
  io.except.e := io.req.stb && enable && Mux(tlbRes.hit, pageFault, ptwFault)
  io.except.assistVal := io.req.addr
  if (config.isInst) {
    io.except.code := ExceptCode.INST_PAGE
  } else {
    io.except.code := Mux(
      io.req.we,
      ExceptCode.STORE_PAGE,
      ExceptCode.LOAD_PAGE
    )
  }

  io.req.rdData := io.bus.rdData
  io.req.ack := False
  ptw.io.memBus.rdData := io.bus.rdData
  ptw.io.memBus.ack := False
  when(enable & ~regTlbHit & io.req.stb) {
    // page table walker mode
    io.bus.stb := ptw.io.memBus.stb
    io.bus.addr := ptw.io.memBus.addr
    io.bus.we := ptw.io.memBus.we
    io.bus.be := ptw.io.memBus.be
    io.bus.wrData := ptw.io.memBus.wrData
    ptw.io.memBus.ack := io.bus.ack

    when(tlbRes.hit) {
      regTlbHit := True
      regTlbAddr := tlbRes.pte.ppn @@ offset
    }
    when(io.except.e) {
      // ack with exception
      io.req.ack := True
    }
  } otherwise {
    // read data mode
    io.bus.stb := io.req.stb
    io.bus.addr := physAddr(31 downto 0)
    io.bus.we := io.req.we
    io.bus.be := io.req.be
    io.bus.wrData := io.req.wrData
    io.req.ack := io.bus.ack
  }

  when(io.req.ack) {
    // clear state
    regTlbHit := False
  }
}
