package peripheral

import spinal.core._
import spinal.lib.{master, slave}
import spinal.lib.fsm._
import model._
import config.SramConfig

/** Sram controller.
  *
  * Interpret sram bus command to control signal. Manage internal state. 2
  * cycles per write, 2 cycles per read. Requests can be continuous.
  */
class SramController(config: SramConfig) extends Component {
  val io = new Bundle {
    val bus = slave(SramBusBundle())
    val ctrl = master(SramControlBundle())
  }

  val regPrefetch = Reg(new Bundle {
    val valid = Bool()
    val addr = UInt(20 bits)
    val data = UInt(32 bits)
  })
  val regWrBusy = RegInit(False)
  regPrefetch.valid.init(False)

  // default values
  io.ctrl.addr := io.bus.addr
  io.ctrl.wrData := io.bus.wrData
  io.ctrl.ben := ~io.bus.be
  io.ctrl.cen := ~io.bus.stb
  io.ctrl.oen := True
  io.ctrl.wen := True
  io.bus.rdData := regPrefetch.data
  io.bus.ack := False

  when(io.bus.stb) {
    when(io.bus.we) {
      // 2 cycles write
      regWrBusy := ~regWrBusy
      io.bus.ack := regWrBusy
      io.ctrl.wen := ~regWrBusy
      regPrefetch.valid := False
    }.otherwise {
      // prefetch something anyway
      io.ctrl.oen := False
      io.ctrl.ben := B"4'b0000"
      val prefetchAddr = UInt(20 bits)
      when(regPrefetch.valid && regPrefetch.addr === io.bus.addr) {
        // from prefetched
        io.bus.ack := True
        // prefetch next
        prefetchAddr := regPrefetch.addr + 1
      }.otherwise {
        io.bus.ack := False
        prefetchAddr := io.bus.addr
      }
      io.ctrl.addr := prefetchAddr
      regPrefetch.valid := True
      regPrefetch.addr := prefetchAddr
      regPrefetch.data := io.ctrl.rdData
    }
  }
}
