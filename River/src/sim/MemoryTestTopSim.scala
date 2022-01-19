package sim

import spinal.core._
import spinal.core.sim._
import spinal.sim._

object MemoryTestTopSim {
  def main(args: Array[String]) {
    SpinalSimConfig().withWave.compile(new MemoryTestTop).doSim { dut =>
      dut.clockDomain.forkStimulus(2)
      dut.io.instBus.stb #= false
      dut.io.dataBus.stb #= false
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.assertReset()
      dut.clockDomain.waitRisingEdge(3)
      dut.clockDomain.deassertReset()
      dut.clockDomain.waitRisingEdge()

      // inst read
      dut.io.instBus.stb #= true
      dut.io.instBus.be #= 0xf
      dut.io.instBus.we #= false
      dut.io.instBus.addr #= 0x80000000L
      dut.clockDomain.waitRisingEdgeWhere(dut.io.instBus.ack.toBoolean)
      // inst read & data read
      dut.io.instBus.addr #= 0x80000004L
      dut.io.dataBus.stb #= true
      dut.io.dataBus.be #= 0xf
      dut.io.dataBus.we #= false
      dut.io.dataBus.addr #= 0x80400000L
      dut.clockDomain.waitRisingEdgeWhere(dut.io.instBus.ack.toBoolean)
      // inst read & data write
      dut.io.instBus.addr #= 0x80000008L
      dut.io.dataBus.we #= true
      dut.io.dataBus.addr #= 0x80400000L
      dut.io.dataBus.wrData #= 0x5a5a5a5a
      dut.clockDomain.waitRisingEdgeWhere(dut.io.instBus.ack.toBoolean)
      dut.io.instBus.stb #= false
      dut.io.dataBus.addr #= 0x80400004L
      dut.io.dataBus.wrData #= 0xff
      dut.clockDomain.waitRisingEdge()
      // inst(uart) read conflicting with ongoing data write
      dut.io.instBus.stb #= true
      dut.io.instBus.addr #= 0x10000000
      dut.clockDomain.waitRisingEdgeWhere(dut.io.dataBus.ack.toBoolean)
      // pending inst(uart) read conflicting with new data write
      dut.io.dataBus.addr #= 0x80400004L
      dut.io.dataBus.wrData #= 0x12345678
      dut.clockDomain.waitRisingEdgeWhere(dut.io.dataBus.ack.toBoolean)
      dut.io.dataBus.stb #= false
      dut.clockDomain.waitRisingEdge()
      // ongoing inst read conflicting with new data read
      dut.io.dataBus.stb #= true
      dut.io.dataBus.we #= false
      sleep(1)
      var dataIsDone = false
      var instIsDone = false
      while (!(instIsDone && dataIsDone)) {
        if (dut.io.dataBus.ack.toBoolean) dataIsDone = true
        if (dut.io.instBus.ack.toBoolean) instIsDone = true
        dut.clockDomain.waitRisingEdge()
      }
      // shutdown
      dut.io.instBus.stb #= false
      dut.io.dataBus.stb #= false
      dut.clockDomain.waitRisingEdge(2)
    }
  }
}
