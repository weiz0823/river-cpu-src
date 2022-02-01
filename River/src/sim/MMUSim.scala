package sim

import spinal.core._
import spinal.core.sim._
import spinal.sim._

import core.mmu.MMU
import core.csr.PrivilegeEnum

object MMUSim {
  def main(args: Array[String]) {
    SpinalSimConfig().withWave.compile(new MMU).doSim { dut =>
      dut.clockDomain.forkStimulus(2)

      dut.io.satp #= 0x80001000L
      dut.io.priv #= PrivilegeEnum.U
      dut.io.mxr #= true
      dut.io.sum #= true
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.assertReset()
      dut.clockDomain.waitRisingEdge(3)
      dut.clockDomain.deassertReset()
      dut.clockDomain.waitRisingEdge()

      dut.io.ifReq.stb #= true
      dut.io.ifReq.we #= false
      dut.io.ifReq.addr #= 0x0555AAAA
      dut.io.memReq.stb #= false
      dut.clockDomain.waitRisingEdge()
    }
  }
}
