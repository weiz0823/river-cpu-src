package core

import spinal.core._
import model._
import core.csr.CsrWrTypes
import core.csr.PrivilegeEnum
import core.mmu.TLBOp

case class IDBundle() extends Bundle {
  val calcType = AluCalcType()
  val aluInput = AluInputBundle()
  val memRw = MemRWBundle()
  val regWb = RegWriteBackBundle()
  val instFlush = Bool()
  val csrWrType = CsrWrTypes()
  val pc = UInt(32 bits)
  val inst = Bits(32 bits)
  val except = ExceptBundle()
  val trapRet = TrapRetBundle()
  val tlbReqOp = TLBOp()
}

/** Registers of ID->EX stage.
  */
class IDRegs extends Component {
  val io = new Bundle {
    val id = in(IDBundle())
    val ex = out(Reg(IDBundle()))
    val idStall = in(Bool())
    val exStall = in(Bool())
  }

  io.ex.instFlush.init(True)

  when(!io.exStall) {
    // should give something
    io.ex := io.id
    when(io.idStall) {
      // insert bubble
      io.ex.memRw.disable()
      io.ex.regWb.disable()
      io.ex.instFlush := True
    }
  }
}
