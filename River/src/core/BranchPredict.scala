package core

import spinal.core._
import model._

class BranchPredict extends Component {
  val io = new Bundle {
    val inst = in(Bits(32 bits))
    val pc = in(UInt(32 bits))
    val predict = out(BranchPredictBundle())
  }

  val isBranch = io.inst(6 downto 0) === B"7'b1100011"

  val offset = B(
    32 bits,
    (31 downto 13) -> True,
    12 -> io.inst(31),
    11 -> io.inst(7),
    (10 downto 5) -> io.inst(30 downto 25),
    (4 downto 1) -> io.inst(11 downto 8),
    0 -> False
  ).asUInt

  io.predict.addr := io.pc + offset
  io.predict.take := isBranch & io.inst(31)
}
