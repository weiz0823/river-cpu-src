package bus

import spinal.core._
import spinal.lib.{master, slave}
import model._
import config.MemoryMapConfig

object MemDeviceEnum extends SpinalEnum {
  val NONE, INVALID = newElement()
  val BASE_RAM, EXT_RAM, UART = newElement()
  val CLINT = newElement()
}

/** Memory controller. Bus interface unit. Require aligned access.
  */
class MemoryController(config: MemoryMapConfig) extends Component {
  val io = new Bundle {
    val instBus = slave(InternalBusBundle())
    val dataBus = slave(InternalBusBundle())
    val baseRamBus = master(SramBusBundle())
    val extRamBus = master(SramBusBundle())
    val uartBus = master(UartBusBundle())
    val clintBus = master(CLINTBusBundle())
  }

  val instDevSelect = new DevTypeSelector(config)
  val dataDevSelect = new DevTypeSelector(config)
  instDevSelect.io.addr := io.instBus.addr
  dataDevSelect.io.addr := io.dataBus.addr
  val instDev = Mux(io.instBus.stb, instDevSelect.io.t, MemDeviceEnum.NONE)
  val dataDev = Mux(io.dataBus.stb, dataDevSelect.io.t, MemDeviceEnum.NONE)

  val extRamMasterSel = new MemDevMasterPrioSelect
  val baseRamAndUartMasterSel = new MemDevMasterPrioSelect
  val clintMasterSel = new MemDevMasterPrioSelect
  extRamMasterSel.io.bus.stb(1) :=
    (dataDev === MemDeviceEnum.EXT_RAM)
  extRamMasterSel.io.bus.stb(0) :=
    (instDev === MemDeviceEnum.EXT_RAM)
  extRamMasterSel.io.ctrl.ack := io.extRamBus.ack
  baseRamAndUartMasterSel.io.bus.stb(1) :=
    (dataDev === MemDeviceEnum.BASE_RAM) | (dataDev === MemDeviceEnum.UART)
  baseRamAndUartMasterSel.io.bus.stb(0) :=
    (instDev === MemDeviceEnum.BASE_RAM) | (instDev === MemDeviceEnum.UART)
  baseRamAndUartMasterSel.io.ctrl.ack := io.uartBus.ack | io.baseRamBus.ack
  clintMasterSel.io.bus.stb(1) :=
    (dataDev === MemDeviceEnum.CLINT)
  clintMasterSel.io.bus.stb(0) :=
    (instDev === MemDeviceEnum.CLINT)
  clintMasterSel.io.ctrl.ack := io.clintBus.ack

  object connectBus {
    def apply(bus1: InternalBusBundle, bus2: SramBusBundle): Unit = {
      bus1.ack := bus2.ack
      bus1.rdData := bus2.rdData
      bus2.wrData := bus1.wrData
      bus2.addr := bus1.addr(2, 20 bits)
      bus2.be := bus1.be
      bus2.we := bus1.we
      bus2.stb := bus1.stb
    }
    def apply(bus1: InternalBusBundle, bus2: UartBusBundle): Unit = {
      bus1.ack := bus2.ack
      bus1.rdData := bus2.rdData
      bus2.wrData := bus1.wrData
      bus2.addr := bus1.addr(2, 1 bits)
      bus2.be := bus1.be
      bus2.we := bus1.we
      bus2.stb := bus1.stb
    }
    def apply(bus1: InternalBusBundle, bus2: CLINTBusBundle): Unit = {
      bus1.ack := bus2.ack
      bus1.rdData := bus2.rdData
      bus2.wrData := bus1.wrData
      bus2.addr := bus1.addr(2, 14 bits)
      bus2.be := bus1.be
      bus2.we := bus1.we
      bus2.stb := bus1.stb
    }
  }

  io.instBus.err := False
  io.dataBus.err := False
  when(instDev === MemDeviceEnum.INVALID) {
    io.instBus.err := True
  }
  when(dataDev === MemDeviceEnum.INVALID) {
    io.dataBus.err := True
  }

  // default values
  io.instBus.ack := False
  io.instBus.rdData := io.baseRamBus.rdData
  io.dataBus.ack := False
  io.dataBus.rdData := io.baseRamBus.rdData
  io.baseRamBus.disable()
  io.baseRamBus.addr := io.dataBus.addr(config.baseRam.bits - 1 downto 2)
  io.baseRamBus.be := io.dataBus.be
  io.baseRamBus.we := io.dataBus.we
  io.baseRamBus.wrData := io.dataBus.wrData
  io.extRamBus.disable()
  io.extRamBus.addr := io.dataBus.addr(config.extRam.bits - 1 downto 2)
  io.extRamBus.be := io.dataBus.be
  io.extRamBus.we := io.dataBus.we
  io.extRamBus.wrData := io.dataBus.wrData
  io.uartBus.disable()
  io.uartBus.addr := io.dataBus.addr(config.uart.bits - 1 downto 2)
  io.uartBus.be := io.dataBus.be
  io.uartBus.we := io.dataBus.we
  io.uartBus.wrData := io.dataBus.wrData
  io.clintBus.disable()
  io.clintBus.addr := io.dataBus.addr(config.clint.bits - 1 downto 2)
  io.clintBus.be := io.dataBus.be
  io.clintBus.we := io.dataBus.we
  io.clintBus.wrData := io.dataBus.wrData

  when(extRamMasterSel.io.sel(1)) {
    connectBus(io.dataBus, io.extRamBus)
  }
  when(extRamMasterSel.io.sel(0)) {
    connectBus(io.instBus, io.extRamBus)
  }
  when(baseRamAndUartMasterSel.io.sel(1)) {
    when(dataDev === MemDeviceEnum.BASE_RAM) {
      connectBus(io.dataBus, io.baseRamBus)
    }.otherwise {
      connectBus(io.dataBus, io.uartBus)
    }
  }
  when(baseRamAndUartMasterSel.io.sel(0)) {
    when(instDev === MemDeviceEnum.BASE_RAM) {
      connectBus(io.instBus, io.baseRamBus)
    }.otherwise {
      connectBus(io.instBus, io.uartBus)
    }
  }
  when(clintMasterSel.io.sel(1)) {
    connectBus(io.dataBus, io.clintBus)
  }
  when(clintMasterSel.io.sel(0)) {
    connectBus(io.instBus, io.clintBus)
  }
}

class DevTypeSelector(config: MemoryMapConfig) extends Component {
  val io = new Bundle {
    val addr = in UInt (32 bits)
    val t = out(MemDeviceEnum)
  }
  when(
    io.addr(31 downto config.extRam.bits)
      === config.extRam.base >> config.extRam.bits
  ) {
    // ext ram
    io.t := MemDeviceEnum.EXT_RAM
  }.elsewhen(
    io.addr(31 downto config.baseRam.bits)
      === config.baseRam.base >> config.baseRam.bits
  ) {
    // base ram
    io.t := MemDeviceEnum.BASE_RAM
  }.elsewhen(
    io.addr(31 downto config.uart.bits)
      === config.uart.base >> config.uart.bits
  ) {
    // uart
    io.t := MemDeviceEnum.UART
  }.elsewhen(
    io.addr(31 downto config.clint.bits)
      === config.clint.base >> config.clint.bits
  ) {
    // Core local interrupt
    io.t := MemDeviceEnum.CLINT
  }.otherwise {
    // invalid
    io.t := MemDeviceEnum.INVALID
  }
}

/** Priority select. Higher bit selected first.
  */
class MemDevMasterPrioSelect extends Component {
  val io = new Bundle {
    val bus = new Bundle {
      val stb = in Bits (2 bits)
    }
    val ctrl = new Bundle {
      val ack = in(Bool())
    }
    val sel = out Bits (2 bits)
  }

  val regSel = RegInit(B(0, 2 bits))
  val regAck = RegNext(io.ctrl.ack)
  val stbOrR = io.bus.stb.orR
  val selCycle = stbOrR.rise() | regAck

  when(selCycle) {
    when(io.bus.stb(1)) {
      io.sel := B"10"
    }.elsewhen(io.bus.stb(0)) {
      io.sel := B"01"
    }.otherwise {
      io.sel := 0
    }
    regSel := io.sel
  }.otherwise {
    io.sel := regSel
  }
}

/** [DEPRECATED] Round-robin master select.
  */
class _MemDevMasterRRSelect extends Component {
  val io = new Bundle {
    val bus = new Bundle {
      val stb = in Bits (2 bits)
    }
    val ctrl = new Bundle {
      val ack = in(Bool())
    }
    val sel = out UInt (1 bits)
    val selE = out(Bool())
  }

  val lastServe = Reg(UInt(1 bits)) init 0
  val regSel = Reg(UInt(1 bits)) init 0
  val regSelE = RegInit(False)
  val regAck = RegNext(io.ctrl.ack)
  val stbOrR = io.bus.stb.orR
  val selCycle = stbOrR.rise() | regAck

  when(selCycle) {
    io.selE := stbOrR
    regSelE := stbOrR
    when(io.bus.stb(lastServe + 1)) {
      lastServe := lastServe + 1
      io.sel := lastServe + 1
      regSel := lastServe + 1
    }.elsewhen(io.bus.stb(lastServe)) {
      io.sel := lastServe
      regSel := lastServe
    }.otherwise {
      io.sel := 0
      regSel := 0
    }
  }.otherwise {
    io.sel := regSel
    io.selE := regSelE
  }
}
