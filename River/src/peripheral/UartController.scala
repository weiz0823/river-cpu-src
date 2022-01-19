package peripheral

import spinal.core._
import spinal.lib.{master, slave}
import spinal.lib.fsm._
import model._
import config.UartConfig

/** Uart controller.
  *
  * Interpret uart bus command to control signal. Manage internal state. Write
  * command will be blocked until transmitter is available. Read command will
  * get garbage if data is not ready.
  */
class UartController(config: UartConfig) extends Component {
  val io = new Bundle {
    val uart = master(UartControlBundle())
    val bus = slave(UartBusBundle())
  }

  val regTxBusy = RegInit(False)
  val regRxBusy = RegInit(False)
  val canWrite = io.uart.tbre & io.uart.tsre

  // UART 16550 registers: https://www.lammertbies.nl/comm/info/serial-uart
  val regLineCtrl = RegInit(U(0, 8 bits)) // LCR
  val regIntE = RegInit(U(0, 8 bits)) // IER

  io.uart.ren := True
  io.uart.wen := True
  io.uart.wrData := io.bus.wrData(7 downto 0)
  io.bus.rdData := 0
  io.bus.ack := False

  val DLAB = regLineCtrl(7)
  val LSR = U(8 bits, 5 -> canWrite, 0 -> io.uart.ready, default -> False)

  when(io.bus.stb && !io.bus.we) {
    when(io.bus.addr === 1) {
      // MCR, LSR, MSR, SCR
      io.bus.ack := True
      io.bus.rdData(15 downto 8) := LSR
    }.otherwise {
      // addr === 0
      when(DLAB) {
        // DLL, DLM, IIR/FCR, LCR
        io.bus.ack := True
      }.otherwise {
        // RBR, IER, IIR/FCR, LCR
        // receive data, assume data is ready (or get garbage)
        // keep connected and wait for next cycle to ack
        io.bus.ack := regRxBusy
        regRxBusy := ~regRxBusy
        io.uart.ren := False
        io.bus.rdData(15 downto 8) := regIntE
        io.bus.rdData(7 downto 0) := io.uart.rdData
      }
      io.bus.rdData(31 downto 24) := regLineCtrl
    }
  }

  when(io.bus.stb && io.bus.we) {
    when(io.bus.addr === 1) {
      // MCR, LSR, MSR, SCR
      io.bus.ack := True
    }.otherwise {
      // addr === 0
      when(DLAB) {
        // DLL, DLM, IIR/FCR, LCR
        io.bus.ack := True
      }.otherwise {
        // RBR, IER, IIR/FCR, LCR
        when(io.bus.be(0)) {
          io.uart.wen := False
          io.bus.ack := regTxBusy
          regTxBusy := ~regTxBusy
        }.otherwise {
          io.bus.ack := True
        }
        when(io.bus.ack & io.bus.be(1)) {
          // IER
          regIntE := io.bus.wrData(15 downto 8)
        }
      }
      when(io.bus.ack & io.bus.be(3)) {
        // LCR
        regLineCtrl := io.bus.wrData(31 downto 24)
      }

    }
  }

}
