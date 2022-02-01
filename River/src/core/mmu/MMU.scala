package core.mmu

import spinal.core._
import spinal.lib._
import model._

import core.csr.PrivilegeEnum

/** Not finished yet. full of bugs.
  */
class MMU extends Component {
  val io = new Bundle {
    val instBus = master(InternalBusBundle())
    val dataBus = master(InternalBusBundle())
    val ifReq = slave(InternalBusBundle())
    val memReq = slave(InternalBusBundle())

    val ifExcept = out(ExceptBundle())
    val memExcept = out(ExceptBundle())

    //// from CSRs
    val satp = in(UInt(32 bits))
    val priv = in(PrivilegeEnum())
    val mxr = in(Bool())
    val sum = in(Bool())
  }

  val iTlb = new TLB(4, 2)
  val dTlb = new TLB(4, 4)

  val iPtw = new PageTableWalker
  val dPtw = new PageTableWalker

  val priv = io.priv
  val enable = io.satp(31) && priv =/= PrivilegeEnum.M
  val pdtBase = io.satp(21 downto 0) @@ U(0, 12 bits) // 34-bit physical address

  def connectTlbQuery(tlb: TLB, vpn: UInt): TLBQueryResult = {
    tlb.io.query.vpn := vpn

    val result = TLBQueryResult()
    result.hit := tlb.io.query.hit
    result.pte := tlb.io.query.pte
    result
  }

  val ifAddr = io.ifReq.addr
  val memAddr = io.memReq.addr
  val iVpn = ifAddr(31 downto 12)
  val dVpn = memAddr(31 downto 12)
  val iOffset = ifAddr(11 downto 0)
  val dOffset = memAddr(11 downto 0)

  val iTlbRes = connectTlbQuery(iTlb, iVpn)
  val dTlbRes = connectTlbQuery(dTlb, dVpn)

  iTlb.io.req := iPtw.io.tlbReq
  dTlb.io.req := dPtw.io.tlbReq

  val iPtwFault = iPtw.io.ack && iPtw.io.fault
  val dPtwFault = dPtw.io.ack && dPtw.io.fault

  val abortRefill = RegNext(dPtwFault)
  iPtw.io.e := !iTlbRes.hit && io.ifReq.stb
  iPtw.io.vpn := iVpn
  iPtw.io.basePPN := pdtBase(33 downto 12)
  iPtw.io.abort := abortRefill

  dPtw.io.e := !dTlbRes.hit && io.memReq.stb
  dPtw.io.vpn := dVpn
  dPtw.io.basePPN := pdtBase(33 downto 12)
  dPtw.io.abort := False

  val addrMisalignCheck = new Area {
    // TODO
  }

  val isUser = (priv === PrivilegeEnum.U)
  val userCheck = new Area {
    val iOk, dOk = Bool()

    iOk := !(iTlbRes.pte.U ^ isUser)

    when(dTlbRes.pte.U) {
      dOk := io.sum || isUser
    } otherwise {
      dOk := !isUser
    }
  }

  // basic V & R bit check is already handled by PTW (R || X)
  val readPermOk = iTlbRes.pte.R || io.mxr // X && (Make eXecutable Readable)
  val writePermOk = !io.memReq.we || dTlbRes.pte.W
  val rwOk = readPermOk && writePermOk

  val iPageFault = !iTlbRes.pte.X || !userCheck.iOk
  val dPageFault = !rwOk || !userCheck.dOk

  io.ifExcept.e := enable && Mux(iTlbRes.hit, iPageFault, iPtwFault)
  io.ifExcept.code := ExceptCode.INST_PAGE
  io.ifExcept.assistVal := io.ifReq.addr

  io.memExcept.e := enable && Mux(dTlbRes.hit, dPageFault, dPtwFault)
  io.memExcept.code := Mux(
    io.ifReq.we,
    ExceptCode.STORE_PAGE,
    ExceptCode.LOAD_PAGE
  )
  io.memExcept.assistVal := io.memReq.addr

  io.instBus.stb := io.ifReq.stb
  io.instBus.we := False
  io.instBus.be := B"4'b1111"
  io.instBus.wrData := 0

  io.dataBus.stb := io.memReq.stb
  io.dataBus.we := io.memReq.we
  io.dataBus.be := io.memReq.be
  io.dataBus.wrData := io.memReq.wrData

  io.ifReq.rdData := io.instBus.rdData
  io.memReq.rdData := io.dataBus.rdData

  iPtw.io.memBus.rdData := io.instBus.rdData
  iPtw.io.memBus.ack := io.instBus.ack

  dPtw.io.memBus.rdData := io.dataBus.rdData
  dPtw.io.memBus.ack := io.dataBus.ack

  when(enable) {
    when(iTlbRes.hit) {
      io.instBus.addr := (iTlbRes.pte.ppn @@ iOffset).resized
      io.ifReq.ack := iPageFault || io.instBus.ack
    } otherwise { // instruction TLB refill
      io.instBus.stb := iPtw.io.memBus.stb
      io.instBus.addr := iPtw.io.memBus.addr

      io.ifReq.ack := iPtwFault
    }

    when(dTlbRes.hit) {
      io.dataBus.addr := (dTlbRes.pte.ppn @@ dOffset).resized
      io.memReq.ack := dPageFault || io.dataBus.ack
    } otherwise { // data TLB refill
      io.dataBus.stb := dPtw.io.memBus.stb
      io.dataBus.addr := dPtw.io.memBus.addr
      io.dataBus.we := False
      io.dataBus.be := B"4'b1111"

      io.memReq.ack := dPtwFault
    }
  } otherwise { // paging not enabled, no virtual address translation
    io.instBus.addr := io.ifReq.addr
    io.dataBus.addr := io.memReq.addr

    io.ifReq.ack := io.instBus.ack
    io.memReq.ack := io.dataBus.ack
  }
}
