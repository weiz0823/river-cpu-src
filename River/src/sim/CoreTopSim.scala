package sim

import spinal.core._
import spinal.core.sim._
import spinal.sim._
import core.CoreTop
import model.InternalBusBundle
import config.CoreConfig
import config.CsrConfig

object CoreTopSim {
  def main(args: Array[String]) {
    val rtl = { () => new CoreTop(CoreConfig(), CsrConfig()) }
    SpinalSimConfig().withWave.compile(rtl()).doSim { dut =>
      dut.clockDomain.forkStimulus(period = 4)
      dut.io.softInt #= false
      dut.io.timeInt #= false
      dut.io.extInt #= false
      dut.io.timeCsrData #= 0

      val program = Vector(0x293L, 0x313L, 0x1000393L, 0x80400537L, 0x128293L,
        0x530333L, 0xfe729ce3L, 0x652023L, 0x13L, 0x13L, 0x13L)

      val instProvider =
        DataProvider(0x80000000L, program)
      val dataProvider =
        DataProvider(0x80400000L, Vector.fill(0x10)(0x12345678))
      val timeout = 200
      var timer = 0

      var instTimer = 0
      var dataTimer = 0

      // reset
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.assertReset()
      dut.clockDomain.waitRisingEdge(10)
      dut.clockDomain.deassertReset()

      while (
        timer < timeout && (!dut.io.instBus.stb.toBoolean || instProvider
          .isValidAddr(dut.io.instBus.addr.toLong))
      ) {
        sleep(1)
        instTimer = processRW(dut.io.instBus, instProvider, instTimer)
        dataTimer = processRW(dut.io.dataBus, dataProvider, dataTimer)

        dut.clockDomain.waitRisingEdge()
        timer += 1
      }

      for (i <- 1 to 10) {
        sleep(1)
        instTimer = processRW(dut.io.instBus, instProvider, instTimer)
        dataTimer = processRW(dut.io.dataBus, dataProvider, dataTimer)

        dut.clockDomain.waitRisingEdge()
        timer += 1
      }

      println("Sim done in", timer, "cycles")
      println(dataProvider.dataset)
    }
  }

  def processRW(
      bus: InternalBusBundle,
      provider: DataProvider,
      oldTimer: Int
  ): Int = {
    val readCycles = 2
    val writeCycles = 2
    var timer = oldTimer

    bus.ack #= false
    if (bus.stb.toBoolean) {
      timer += 1
      if (bus.we.toBoolean) {
        if (timer == writeCycles) {
          timer = 0
          printf("write %x\n", bus.addr.toLong)
          provider.set(bus.addr.toLong, bus.wrData.toLong, bus.be.toInt)
          bus.ack #= true
        }
      } else {
        if (timer == readCycles) {
          timer = 0
          printf("read %x\n", bus.addr.toLong)
          bus.rdData #= provider.get(bus.addr.toLong)
          bus.ack #= true
        }
      }
    }
    timer
  }
}
