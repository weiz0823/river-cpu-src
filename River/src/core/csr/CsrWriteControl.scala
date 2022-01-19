package core.csr

import spinal.core._
import model.CsrRWBundle

class CsrWriteControl extends Component {
  val io = new Bundle {
    val wrData = in(UInt(32 bits))
    val wrType = in(CsrWrTypes())
    val current = in(UInt(32 bits))
    val next = out(UInt(32 bits))
    val e = out(Bool())
  }

  val wrMask = io.wrType.mux(
    CsrWrTypes.NONE -> U(32 bits, default -> false),
    CsrWrTypes.WRITE -> U(32 bits, default -> true),
    default -> io.wrData
  )
  val wrVal = io.wrType.mux(
    CsrWrTypes.CLEAR -> U(32 bits, default -> false),
    CsrWrTypes.SET -> U(32 bits, default -> true),
    default -> io.wrData
  )
  io.e := io.wrType =/= CsrWrTypes.NONE
  for (i <- 0 to 31)
    io.next(i) := Mux(wrMask(i), wrVal(i), io.current(i))

  def connectCsrRW(csr: CsrRWBundle) {
    io.wrData := csr.wrData
    io.wrType := csr.wrType
  }
}
