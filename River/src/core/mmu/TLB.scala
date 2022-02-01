package core.mmu

import spinal.core._
import spinal.lib._

case class InternalPTE() extends Bundle {
  val ppn = UInt(22 bits)
  val R = Bool()
  val W = Bool()
  val X = Bool()
  val U = Bool()
  val G = Bool()
  // bit V, A, D and field RSW omitted
}

case class PTE() extends Bundle {
  val V = Bool()
  val R = Bool()
  val W = Bool()
  val X = Bool()
  val U = Bool()
  val G = Bool()
  val A = Bool()
  val D = Bool()
  val rsw = Bits(2 bit)
  val ppn = UInt(22 bits)
}

object TLBOp extends SpinalEnum {
  val NOP, INSERT, INVALIDATE, INVALIDATE_ALL = newElement()
}

// TLB write
case class TLBRequest() extends Bundle with IMasterSlave {
  val op = TLBOp()
  val vpn = UInt(20 bits)
  val pte = Bits(32 bits) // PTE value from memory

  override def asMaster() {
    out(op, vpn, pte)
  }

  def disable() {
    op := TLBOp.NOP
  }
}

// TLB read
case class TLBQuery() extends Bundle with IMasterSlave {
  val vpn = UInt(20 bits)
  val hit = Bool()
  val pte = InternalPTE()

  override def asMaster() {
    out(vpn)
    in(hit, pte)
  }
}

case class TLBQueryResult() extends Bundle {
  val hit = Bool()
  val pte = InternalPTE()
}

case class TLBEntry() extends Bundle {
  val valid = Bool()
  val vpn = UInt(20 bits)
  val pte = InternalPTE()
}

class TLB(nSets: Int, nWays: Int) extends Component {
  val io = new Bundle {
    val query = slave(TLBQuery()) // read
    val req = slave(TLBRequest()) // write
  }

  val nSetsWidth = log2Up(nSets)
  val nWaysWidth = log2Up(nWays)

  val entries = Vec(Vec(Reg(TLBEntry()), size = nWays), size = nSets)
  entries.foreach(ff => ff.foreach(f => f.valid.init(False)))
  val counter = Reg(UInt(nWaysWidth bits)) init (0)
  counter := counter + 1

  def vpnIndex(vpn: UInt): UInt = {
    val vpn0 = vpn(9 downto 0)
    val vpn1 = vpn(19 downto 10)
    (vpn1 ^ vpn0)(nSetsWidth - 1 downto 0)
  }

  def matchEntry(vpn: UInt)(e: TLBEntry) = e.valid && e.vpn === vpn
  def matchedPte(vpn: UInt)(e: TLBEntry) = matchEntry(vpn)(e) ? e.pte.asBits | 0

  val tlbQuery = new Area { // should be completely combinatorial
    val index = vpnIndex(io.query.vpn)
    val hit = entries(index).map(matchEntry(io.query.vpn)).reduce(_ || _)
    val pteBits = entries(index).map(matchedPte(io.query.vpn)).reduce(_ | _)

    io.query.hit := hit
    io.query.pte.assignFromBits(pteBits)
  }

  val tlbUpdate = new Area {
    def assignInternalPte(pte: InternalPTE, raw: Bits) {
      pte.ppn := raw(31 downto 10).asUInt
      pte.R := raw(1)
      pte.W := raw(2)
      pte.X := raw(3)
      pte.U := raw(4)
      pte.G := raw(5)
    }

    def findLastBit(x: Bits): UInt = {
      val n = x.getWidth
      val width = log2Up(n) bits
      val result = Vec(UInt(width), n + 1)
      result(0) := 0
      for (i <- 0 until n)
        result(i + 1) := x(i) ? U(i, width) | result(i)
      result(n)
    }

    def findFirstBit(x: Bits): UInt = {
      val n = x.getWidth
      PriorityMux[UInt](
        x,
        Array.range(0, n).map({ i => U(i, log2Up(n) bits) })
      )
    }

    val index = vpnIndex(io.req.vpn)
    val matches = entries(index).map(matchEntry(io.req.vpn))
    val hit = matches.reduce(_ || _)

    switch(io.req.op) {
      is(TLBOp.INSERT) {
        val slots = entries(index).map(e => !e.valid).asBits
        val way = slots.orR ? findFirstBit(slots) | counter
        // if all entries are occupied, choose one "randomly" (?)

        when(!hit) {
          entries(index)(way).valid := True
          entries(index)(way).vpn := io.req.vpn
          assignInternalPte(entries(index)(way).pte, io.req.pte)
        }
      }
      is(TLBOp.INVALIDATE) {
        // WARNING: all entries matching given vpn are invalidated
        // but there *should* be no more than 1 matches
        // should we locate the only entry instead?

        // NOTE: should we raise some exception like "Machine Check"
        // when there are more than 1 matches?
        for (i <- 0 until nWays) {
          when(matches(i)) {
            entries(index)(i).valid := False
          }
        }
      }
      is(TLBOp.INVALIDATE_ALL) {
        for (i <- 0 until nSets) {
          for (j <- 0 until nWays) {
            when(!entries(i)(j).pte.G) {
              entries(i)(j).valid := False
            }
          }
        }
      }
      default {
        // no operation
      }
    }
  }
}
