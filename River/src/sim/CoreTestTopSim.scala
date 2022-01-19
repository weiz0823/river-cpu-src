package sim

import spinal.core._
import spinal.core.sim._
import spinal.sim._

object CoreTestTopSim {
  def main(args: Array[String]) {
    SpinalSimConfig().withWave.compile(new CoreTestTop).doSim { dut =>
      dut.clockDomain.forkStimulus(period = 2)

      // reset
      dut.clockDomain.assertReset()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.deassertReset()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()

      dut.io.inst #= ADDI(1, 0, 1)
      dut.clockDomain.waitRisingEdge()
      // EX data shortcut to ID
      dut.io.inst #= genInst.XORI(1, 1, 0x5a5a5)
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      // MEM data shortcut to ID
      dut.io.inst #= genInst.ORI(1, 1, 0xf)
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      // WB data shortcut to ID
      dut.io.inst #= genInst.ANDI(1, 1, 0xffff0)
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      // no data shortcut
      dut.io.inst #= genInst.SLTI(2, 1, 0)
      dut.clockDomain.waitRisingEdge()
      // no data conflict
      dut.io.inst #= genInst.SLTIU(2, 1, 0)
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= genInst.SLLI(0, 1, 1)
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= genInst.SRLI(0, 1, 4)
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= genInst.SRAI(0, 1, 4)
      dut.clockDomain.waitRisingEdge()
      // ensure pipeline clean
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
      dut.io.inst #= 0
      dut.clockDomain.waitRisingEdge()
    }
  }
}
