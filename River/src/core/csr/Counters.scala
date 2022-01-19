package core.csr

import spinal.core._
import spinal.lib.slave
import model._

/** Read-only performance counters.
  */
class Counters extends Component {
  val io = new Bundle {
    val timeData = in(UInt(64 bits))
    val currentInstValid = in(Bool())
    val csr = slave(CounterCsrBundle())
  }

  val regCycle = Reg(UInt(64 bits)) init 0
  val regInstRet = Reg(UInt(64 bits)) init 0

  regCycle := regCycle + 1
  when(io.currentInstValid) { regInstRet := regInstRet + 1 }

  io.csr.time.rdData := io.timeData(31 downto 0)
  io.csr.time.instValid := (io.csr.time.priv === PrivilegeEnum.U)
  io.csr.timeH.rdData := io.timeData(63 downto 32)
  io.csr.timeH.instValid := (io.csr.timeH.priv === PrivilegeEnum.U)
  io.csr.cycle.rdData := regCycle(31 downto 0)
  io.csr.cycle.instValid := (io.csr.cycle.priv === PrivilegeEnum.U)
  io.csr.cycleH.rdData := regCycle(63 downto 32)
  io.csr.cycleH.instValid := (io.csr.cycleH.priv === PrivilegeEnum.U)
  io.csr.instRet.rdData := regInstRet(31 downto 0)
  io.csr.instRet.instValid := (io.csr.instRet.priv === PrivilegeEnum.U)
  io.csr.instRetH.rdData := regInstRet(63 downto 32)
  io.csr.instRetH.instValid := (io.csr.instRetH.priv === PrivilegeEnum.U)
}
