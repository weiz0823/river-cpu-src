package sim

import spinal.core.sim._

object MemHWMasterSelectSim {
  def main(args: Array[String]) {
    SpinalSimConfig().withWave.compile(new bus._MemDevMasterRRSelect).doSim {
      dut =>
        dut.clockDomain.forkStimulus(2)
        dut.io.bus.stb #= 0
        dut.io.ctrl.ack #= false
        dut.clockDomain.waitRisingEdge()
        dut.clockDomain.assertReset()
        dut.clockDomain.waitRisingEdge(3)
        dut.clockDomain.deassertReset()
        dut.clockDomain.waitRisingEdge()

        dut.io.bus.stb #= 3
        dut.io.ctrl.ack #= true
        dut.clockDomain.waitRisingEdge(4)
        dut.io.bus.stb #= 1
        dut.clockDomain.waitRisingEdge()
        dut.io.bus.stb #= 2
        dut.clockDomain.waitRisingEdge()
        dut.io.bus.stb #= 0
        dut.io.ctrl.ack #= false
        dut.clockDomain.waitRisingEdge(3)
    }
  }
}
