package peripheral

import spinal.core._
import spinal.lib.slave
import model.CLINTBusBundle
import spinal.lib.Timeout
import spinal.lib.CounterFreeRun

class CoreLocalInt extends Component {
  val io = new Bundle {
    val bus = slave(CLINTBusBundle())
    val softInt = out(Bool())
    val timeInt = out(Bool())
    val timeCsrData = out(UInt(64 bits))
  }
  // warn: if RDTIME follows write to mtime (MMIO), then RDTIME will get old time value

  // software interrupt
  val regSoftInt = RegInit(False)
  io.softInt := regSoftInt

  // QEMU(10MHz), Spike(2MHz)
  val timer = CounterFreeRun(5) // 50MHz -> 10MHz
  val mtime = RegInit(U(0, 64 bits))
  // almost never interrupt
  val mtimecmp = RegInit(U(64 bits, 63 -> false, default -> true))
  val timeDiff = mtime - mtimecmp
  io.timeInt := ~timeDiff(63)
  io.timeCsrData := mtime

  when(timer.willOverflowIfInc) {
    mtime := mtime + 1
  }

  io.bus.ack := io.bus.stb

  switch(io.bus.addr) {
    is(0) {
      // msip
      io.bus.rdData := (0 -> regSoftInt, default -> False)
      when(io.bus.stb & io.bus.we) {
        when(io.bus.be(0)) {
          regSoftInt := io.bus.wrData(0)
        }
      }
    }
    is(0x1000) {
      // mtimecmp
      io.bus.rdData := mtimecmp(31 downto 0)
      when(io.bus.stb & io.bus.we) {
        for (i <- 0 to 3)
          when(io.bus.be(i)) {
            mtimecmp(i * 8, 8 bits) := io.bus.wrData(i * 8, 8 bits)
          }
      }
    }
    is(0x1001) {
      // mtimecmp(h)
      io.bus.rdData := mtimecmp(63 downto 32)
      when(io.bus.stb & io.bus.we) {
        for (i <- 0 to 3)
          when(io.bus.be(i)) {
            mtimecmp(32 + i * 8, 8 bits) := io.bus.wrData(i * 8, 8 bits)
          }
      }
    }
    is(0x2ffe) {
      // mtime
      io.bus.rdData := mtime(31 downto 0)
      when(io.bus.stb & io.bus.we) {
        for (i <- 0 to 3)
          when(io.bus.be(i)) {
            mtime(i * 8, 8 bits) := io.bus.wrData(i * 8, 8 bits)
          }
      }
    }
    is(0x2fff) {
      // mtime(h)
      io.bus.rdData := mtime(63 downto 32)
      when(io.bus.stb & io.bus.we) {
        for (i <- 0 to 3)
          when(io.bus.be(i)) {
            mtime(32 + i * 8, 8 bits) := io.bus.wrData(i * 8, 8 bits)
          }
      }
    }
    default {
      io.bus.rdData := 0
    }
  }
}
