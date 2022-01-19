package sim

import spinal.core.sim._
import peripheral.UartController
import config.UartConfig

object UartControllerSim {
  def main(args: Array[String]) {
    SpinalSimConfig().withWave.compile(new UartController(UartConfig())).doSim {
      dut =>
        dut.clockDomain.forkStimulus(2)
        dut.clockDomain.waitRisingEdge()
        dut.clockDomain.assertReset()
        dut.clockDomain.waitRisingEdge()
        dut.clockDomain.deassertReset()
        dut.clockDomain.waitRisingEdge()

        dut.io.uart.ready #= false
        dut.io.uart.tbre #= false
        dut.io.uart.tsre #= false
        // read garbage
        dut.io.bus.addr #= 0
        dut.io.bus.we #= false
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.bus.stb #= false
        dut.clockDomain.waitRisingEdge()
        // read status
        dut.io.bus.addr #= 1
        dut.io.bus.we #= false
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.bus.stb #= false
        dut.io.uart.ready #= true
        dut.io.uart.rdData #= 0x5a
        dut.clockDomain.waitRisingEdge()
        // read status
        dut.io.bus.addr #= 1
        dut.io.bus.we #= false
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        // read 0x5a
        dut.io.bus.addr #= 0
        dut.io.bus.we #= false
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.uart.ready #= false
        dut.io.bus.stb #= false
        dut.clockDomain.waitRisingEdge()
        // write 0x6a
        dut.io.bus.addr #= 0
        dut.io.bus.wrData #= 0x6a
        dut.io.bus.we #= true
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        // read status
        dut.io.bus.addr #= 1
        dut.io.bus.we #= false
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.uart.tbre #= true
        dut.io.uart.tsre #= true
        dut.io.bus.stb #= false
        dut.clockDomain.waitRisingEdge()
        // read status
        dut.io.bus.addr #= 1
        dut.io.bus.we #= false
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.uart.tbre #= false
        dut.io.uart.tsre #= false
        // write 0x6b
        dut.io.bus.addr #= 0
        dut.io.bus.wrData #= 0x6b
        dut.io.bus.we #= true
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.uart.tbre #= true
        // write 0x6c when busy
        dut.io.bus.addr #= 0
        dut.io.bus.wrData #= 0x6c
        dut.io.bus.we #= true
        dut.io.bus.stb #= true
        dut.clockDomain.waitRisingEdge(3)
        dut.io.uart.tsre #= true
        dut.io.uart.tbre #= false
        dut.clockDomain.waitRisingEdge()
        dut.io.uart.tsre #= false
        dut.clockDomain.waitRisingEdgeWhere(dut.io.bus.ack.toBoolean)
        dut.io.bus.stb #= false
        dut.clockDomain.waitRisingEdge(5)
    }
  }
}
