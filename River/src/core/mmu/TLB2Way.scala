package core.mmu

import spinal.core._
import spinal.lib.master
import model._

final case class TLBSet2Way() extends Bundle {
  val entries = Vec.fill(2)(TLBEntry())
  // 1 bit access record for pseudo-LRU
  val access = Bool()

  def eldestEntry() = (~access).asUInt
}

/** 2-way TLB. Require nSetShift<=10.
  */
class TLB2Way(nSetShift: Int) extends Component {
  val io = new Bundle {
    // query, purely combinatorial from TLB entries
    val query = new Bundle {
      val e = in(Bool())
      val vpn = in(UInt(20 bits))
      val hit = out(Bool())
      val entry = out(InternalPTE())
    }

    // either refill from PTW, or invalidate from sfence.vma instruction
    val req = in(new Bundle {
      val vpn = in(UInt(20 bits))
      val op = TLBOp()
      val data = UInt(32 bits)
    })
  }

  val nSetWidth = nSetShift.bits

  val sets = Vec.fill(1 << nSetShift)(Reg(TLBSet2Way()))
  sets.foreach(set => set.entries.foreach(entry => entry.valid.init(False)))

  def vpnIndex(vpn: UInt): UInt = (vpn(0, nSetWidth) ^ vpn(10, nSetWidth))

  val selectedSet = sets(vpnIndex(io.query.vpn))
  val setHit = selectedSet.entries.map(entry => entry.hit(io.query.vpn))
  io.query.hit := setHit.reduce(_ | _)
  io.query.entry := Mux(
    setHit(0),
    selectedSet.entries(0).pte,
    selectedSet.entries(1).pte
  )
  when(io.query.e & io.query.hit) {
    selectedSet.access := setHit(1)
  }

  switch(io.req.op) {
    is(TLBOp.INSERT) {
      // we always insert PTE assuming its VPN == io.query.vpn
      // invalid entry will never be accessed, so we insert according to that
      // (not always true if it is 4-way)
      val updateSet = sets(vpnIndex(io.req.vpn))
      val entry = updateSet.entries(updateSet.eldestEntry())
      entry.vpn := io.req.vpn
      entry.pte.parseFromUInt(io.req.data)
      entry.valid := True
    }
    is(TLBOp.INVALIDATE_ALL) {
      // actually we could preserve all entries with G set
      sets.foreach(set => set.entries.foreach(entry => (entry.valid := False)))
    }
  }
}
