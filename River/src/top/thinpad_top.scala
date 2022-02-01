package top

import spinal.core._
import spinal.lib.master
import model._
import spinal.lib.com.uart.UartCtrl
import spinal.lib.IMasterSlave
import config._

/** Design top level. Don't change io signal name.
  *
  * Can also use setName("signal_name") to set signal name
  */
class thinpad_top extends Component {
  noIoPrefix()
  val io = new Bundle {
    val clk_50M = in(Bool())
    val clk_11M0592 = in(Bool())
    val clock_btn = in(Bool())
    val reset_btn = in(Bool())

    val touch_btn = in(Bits(4 bits))
    val dip_sw = in(Bits(32 bits))
    val leds = out(Bits(16 bits))
    val dpy0 = out(Bits(8 bits))
    val dpy1 = out(Bits(8 bits))

    val uart = new Bundle {
      val rdn = out(Bool())
      val wrn = out(Bool())
      val dataready = in(Bool())
      val tbre = in(Bool())
      val tsre = in(Bool())
    }

    val base_ram = master(RamInoutBundle())
    val ext_ram = master(RamInoutBundle())

  }

  val freeRunClockDomain = ClockDomain(
    clock = io.clk_50M,
    frequency = FixedFrequency(50 MHz)
  )
  val freeRunClockArea = new ClockingArea(freeRunClockDomain) {
    // 异步复位，同步释放
    val defaultReset = RegNext(io.reset_btn)
  }

  val defaultClockDomain =
    ClockDomain(
      clock = io.clk_50M,
      reset = freeRunClockArea.defaultReset,
      frequency = FixedFrequency(50 MHz)
    )

  def connectRam(x: RamInoutBundle, y: SramControlBundle) {
    y.rdData := x.data
    x.addr := y.addr
    x.be_n := y.ben
    x.ce_n := y.cen
    x.we_n := y.wen
    x.oe_n := y.oen
  }

  val baseRam = SramControlBundle()
  val extRam = SramControlBundle()
  connectRam(io.base_ram, baseRam)
  connectRam(io.ext_ram, extRam)
  when(~extRam.wen && ~extRam.cen) {
    io.ext_ram.data := extRam.wrData
  }

  val uartCtrl = UartControlBundle()
  when(~baseRam.wen && ~baseRam.cen) {
    io.base_ram.data := baseRam.wrData
  }.elsewhen(~uartCtrl.wen) {
    io.base_ram.data(7 downto 0) := uartCtrl.wrData
  }
  io.uart.rdn := uartCtrl.ren
  io.uart.wrn := uartCtrl.wen
  uartCtrl.rdData := io.base_ram.data(7 downto 0)
  uartCtrl.ready := io.uart.dataready
  uartCtrl.tbre := io.uart.tbre
  uartCtrl.tsre := io.uart.tsre

  val dpyNumber = UInt(8 bits)
  val dpyDot = Bits(2 bits)
  val segDpy0 = new Seg7
  val segDpy1 = new Seg7
  segDpy0.io.num := dpyNumber(3 downto 0)
  segDpy1.io.num := dpyNumber(7 downto 4)
  io.dpy0(7 downto 1) := segDpy0.io.seg
  io.dpy1(7 downto 1) := segDpy1.io.seg
  io.dpy0(0) := dpyDot(0)
  io.dpy1(0) := dpyDot(1)

  val defaultClockArea = new ClockingArea(defaultClockDomain) {
    // components using 50M clock

    val coreConfig = CoreConfig()
    val devConfig = DeviceConfig()
    val memMapConfig = MemoryMapConfig()
    val csrConfig = CsrConfig()

    val cpuCore = new core.CoreTop(coreConfig, csrConfig)
    val memController = new bus.MemoryController(memMapConfig)
    val baseRamController = new peripheral.SramController(devConfig.baseRam)
    val extRamController = new peripheral.SramController(devConfig.extRam)
    val uartController = new peripheral.UartController(devConfig.uart)
    val coreLocalInt = new peripheral.CoreLocalInt

    baseRam <> baseRamController.io.ctrl
    extRam <> extRamController.io.ctrl
    uartCtrl <> uartController.io.uart

    memController.io.dataBus <> cpuCore.io.dataBus
    memController.io.instBus <> cpuCore.io.instBus

    memController.io.baseRamBus <> baseRamController.io.bus
    memController.io.extRamBus <> extRamController.io.bus
    memController.io.uartBus <> uartController.io.bus
    memController.io.clintBus <> coreLocalInt.io.bus

    cpuCore.io.softInt := coreLocalInt.io.softInt
    cpuCore.io.timeInt := coreLocalInt.io.timeInt
    cpuCore.io.extInt := False

    cpuCore.io.timeCsrData := coreLocalInt.io.timeCsrData

    dpyNumber := 0
    dpyDot := 0
    io.leds := 0
  }
}

case class RamInoutBundle() extends Bundle with IMasterSlave {
  val data = Analog(UInt(32 bits))
  val addr = UInt(20 bits)
  val be_n = Bits(4 bits)
  val ce_n = Bool()
  val oe_n = Bool()
  val we_n = Bool()

  override def asMaster(): Unit = {
    inout(data)
    out(addr, be_n, ce_n, oe_n, we_n)
  }

}
