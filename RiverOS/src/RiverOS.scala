import util._
import core.CoreTop
import config.CoreConfig
import config.CsrConfig
import spinal.core.sim._
import spinal.core._
import spinal.sim._
import model.InternalBusBundle
import scala.io.StdIn._
import java.io.BufferedReader
import java.io.InputStreamReader
import scala.collection.mutable.ArrayBuffer
import java.net.Socket
object RiverOS {
  def main(args: Array[String]): Unit = {
    val rtl = () => new CoreTop(CoreConfig(), CsrConfig())
    SpinalSimConfig() /*.withWave*/.compile(rtl()).doSim { dut =>
      dut.clockDomain.forkStimulus(period = 2)

      println("\n\n")
      println(
        raw" _______    _____ ____   ____ _________ _______      ____    _______ "
      )
      println(
        raw"|_   __ \  |_   _|_  _| |_  _|_   ___  |_   __ \   .'    \. /  ___  |"
      )
      println(
        raw"  | |__) |   | |   \ \   / /   | |_  \_| | |__) | /  .--.  \  (__ \_|"
      )
      println(
        raw"  |  __ /    | |    \ \ / /    |  _|  _  |  __ /  | |    | |'.___\-. "
      )
      println(
        raw" _| |  \ \_ _| |_    \ ' /    _| |___/ |_| |  \ \_\  \--'  /\\____) |"
      )
      println(
        raw"|____| |___|_____|    \_/    |_________|____| |___|\.____.'|_______.'"
      )
      println("\n")

      // load the ram
      val port = 3456
      Log.info(
        s"Loading binary from extRam: '${args(0)}', baseRam: '${args(1)}', listening on p$port"
      )
      val ioCtr = IOControl(args(0), args(1), port)
      Log.info("Connected to terminal")

      dut.io.softInt #= false
      dut.io.timeInt #= false
      dut.io.extInt #= false
      dut.io.timeCsrData #= 0

      // reset the core
      Log.info("Reseting CoreTop")
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.deassertReset()
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.assertReset()
      dut.clockDomain.waitRisingEdge(5)
      dut.clockDomain.deassertReset()

      var timer = 0
      var ifTimer = 0
      var memTimer = 0
      val timeout = 1 << 30

      // run the simulation
      Log.done("Finish setting up CoreTop, fire up!")
      while (
        timer < timeout && (!dut.io.instBus.stb.toBoolean || (ioCtr
          .isValid(dut.io.instBus.addr.toLong)))
      ) {
        sleep(1)
        ioCtr.clockTick()
        dut.io.softInt #= ioCtr.clintModel.hasSoftInt()
        dut.io.timeInt #= ioCtr.clintModel.hasTimerInt()
        dut.io.timeCsrData #= ioCtr.clintModel.getTime()
        ifTimer = process(dut.io.instBus, ioCtr, ifTimer, 'i')
        memTimer = process(dut.io.dataBus, ioCtr, memTimer, 'm')
        dut.clockDomain.waitRisingEdge()
        timer += 1
      }

      Log.info(
        s"Stopped after $timer cycles, pc = 0x${dut.io.instBus.addr.toLong.toHexString}"
      )

      // uncomment simPublic() in regFile to enable read
      /*for (i <- 0 until 32) {
        println(
          s"${Disassembler.getRegisterString(i)} = ${dut.regFile.regFile(i).toLong.toHexString}"
        )
      }*/

      ioCtr.close()
    }
  }

  def process(
      bus: InternalBusBundle,
      io: IOControl,
      timer: Int,
      mode: Char
  ): Int = {
    val rCycle = 1
    var wCycle = 1
    var t = timer

    bus.ack #= false
    if (bus.stb.toBoolean) {
      t += 1
      if (bus.we.toBoolean) { // write
        if (t == wCycle) {
          t = 0
          if (mode == 'm') {
            io.set(bus.addr.toLong, bus.be.toInt, bus.wrData.toLong)
          }
          bus.ack #= true
        }
      } else {
        if (t == rCycle) {
          t = 0
          bus.rdData #= (if (mode == 'i') {
                           io.getInst(bus.addr.toLong, bus.be.toInt)
                         } else {
                           io.getData(bus.addr.toLong, bus.be.toInt)
                         })
          bus.ack #= true
        }
      }
    }
    t
  }
}
