package sim

import spinal.core.sim._
import peripheral.SramController
import config.SramConfig

object SramControllerSim {
  def main(args: Array[String]) {
    SpinalSimConfig().withWave.compile(new SramController(SramConfig())).doSim {
      dut =>
        dut.clockDomain.forkStimulus(period = 2)

        dut.io.bus.addr #= 0x10
        dut.io.bus.stb #= true
        dut.io.bus.we #= false
        dut.io.bus.be #= 0xf
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.bus.stb #= false
        dut.io.bus.addr #= 0xfffff
        dut.clockDomain.waitRisingEdge()
        // normal continuous reading
        dut.io.bus.addr #= 0x11
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.bus.stb #= false
        dut.io.bus.addr #= 0xfffff
        dut.clockDomain.waitRisingEdge()
        dut.io.bus.addr #= 0x12
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        // trick: one cycle multiple data (not promised to be correct)
        dut.io.bus.addr #= 0x13
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.bus.addr #= 0x14
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.bus.stb #= false
        dut.io.bus.addr #= 0xfffff
        dut.clockDomain.waitRisingEdge(3)
        // write
        dut.io.bus.addr #= 0x15
        dut.io.bus.wrData #= 1
        dut.io.bus.stb #= true
        dut.io.bus.we #= true
        dut.io.bus.be #= 0xf
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.bus.stb #= false
        dut.io.bus.addr #= 0xfffff
        dut.clockDomain.waitRisingEdge()
        dut.io.bus.addr #= 0x16
        dut.io.bus.wrData #= 2
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        // continuous writing
        dut.io.bus.addr #= 0x17
        dut.io.bus.wrData #= 3
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.bus.stb #= false
        dut.io.bus.addr #= 0xfffff
        dut.clockDomain.waitRisingEdge(3)
    }
  }
}
