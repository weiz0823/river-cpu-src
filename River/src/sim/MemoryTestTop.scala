package sim

import spinal.core._
import model._
import peripheral.SramController
import peripheral.UartController
import spinal.lib.{master, slave}
import config._

class MemoryTestTop extends Component {
  val io = new Bundle {
    val instBus = slave(InternalBusBundle())
    val dataBus = slave(InternalBusBundle())
    val baseRam = master(SramControlBundle())
    val extRam = master(SramControlBundle())
    val uart = master(UartControlBundle())
  }

  val devConfig = DeviceConfig()
  val memMapConfig = MemoryMapConfig()

  val memCtrl = new bus.MemoryController(memMapConfig)
  val baseRamCtrl = new SramController(devConfig.baseRam)
  val extRamCtrl = new SramController(devConfig.extRam)
  val uartCtrl = new UartController(devConfig.uart)

  memCtrl.io.baseRamBus <> baseRamCtrl.io.bus
  memCtrl.io.extRamBus <> extRamCtrl.io.bus
  memCtrl.io.uartBus <> uartCtrl.io.bus
  memCtrl.io.instBus <> io.instBus
  memCtrl.io.dataBus <> io.dataBus
  baseRamCtrl.io.ctrl <> io.baseRam
  extRamCtrl.io.ctrl <> io.extRam
  uartCtrl.io.uart <> io.uart
}
