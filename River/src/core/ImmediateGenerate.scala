package core

import spinal.core._

object ImmType extends SpinalEnum {
  val R, I, S, B, U, J = newElement()
}

class ImmediateGenerate extends Component {
  val io = new Bundle {
    val inst = in UInt (32 bits)
    val immType = in(ImmType)
    val imm = out UInt (32 bits)
  }

  val sign = io.inst(31)

  io.imm(31 downto 20) := io.immType.mux(
    ImmType.U -> io.inst(31 downto 20),
    default -> U(12 bits, default -> sign)
  )
  io.imm(19 downto 12) := io.immType.mux(
    ImmType.U -> io.inst(19 downto 12),
    ImmType.J -> io.inst(19 downto 12),
    default -> U(8 bits, default -> sign)
  )
  io.imm(11) := io.immType.mux(
    ImmType.J -> io.inst(20),
    ImmType.B -> io.inst(7),
    ImmType.U -> False,
    default -> sign
  )
  io.imm(10 downto 5) := io.immType.mux(
    ImmType.U -> U(0),
    default -> io.inst(30 downto 25)
  )
  io.imm(4 downto 1) := io.immType.mux(
    ImmType.I -> io.inst(24 downto 21),
    ImmType.J -> io.inst(24 downto 21),
    ImmType.S -> io.inst(11 downto 8),
    ImmType.B -> io.inst(11 downto 8),
    default -> U(0)
  )
  io.imm(0) := io.immType.mux(
    ImmType.I -> io.inst(20),
    ImmType.S -> io.inst(7),
    default -> False
  )
}
