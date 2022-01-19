package core

import spinal.core._
import spinal.lib.master
import model._

/** Memory read and write handling.
  *
  * Assume address is aligned.
  */
class MemRW extends Component {
  val io = new Bundle {
    val memRw = in(MemRWBundle())
    val memRdData = out(UInt(32 bits))
    val stallReq = out(Bool())
    val dataBus = master(InternalBusBundle())
  }

  io.dataBus.we := io.memRw.we
  io.dataBus.addr := io.memRw.addr(31 downto 2) @@ U"2'b00"
  io.dataBus.stb := io.memRw.ce
  io.stallReq := io.memRw.ce & ~io.dataBus.ack

  switch(io.memRw.len) {
    is(MemRWLength.WORD) {
      io.memRdData := io.dataBus.rdData
      io.dataBus.wrData := io.memRw.data
      io.dataBus.be := B"4'b1111"
    }
    is(MemRWLength.HALF) {
      val tmpData = UInt(16 bits)
      val tmpWrData = io.memRw.data(15 downto 0)
      io.dataBus.wrData := 0
      switch(io.memRw.addr(1)) {
        is(False) {
          tmpData := io.dataBus.rdData(15 downto 0)
          io.dataBus.wrData(15 downto 0) := tmpWrData
          io.dataBus.be := B"4'b0011"
        }
        is(True) {
          tmpData := io.dataBus.rdData(31 downto 16)
          io.dataBus.wrData(31 downto 16) := tmpWrData
          io.dataBus.be := B"4'b1100"
        }
      }
      when(io.memRw.isSignExtend) {
        io.memRdData := tmpData.asSInt.resize(32 bits).asUInt
      }.otherwise {
        io.memRdData := tmpData.resized
      }
    }
    is(MemRWLength.BYTE) {
      val tmpData = UInt(8 bits)
      val tmpWrData = io.memRw.data(7 downto 0)
      tmpData := io.memRw
        .addr(1 downto 0)
        .mux(
          0 -> io.dataBus.rdData(7 downto 0),
          1 -> io.dataBus.rdData(15 downto 8),
          2 -> io.dataBus.rdData(23 downto 16),
          3 -> io.dataBus.rdData(31 downto 24)
        )
      io.dataBus.wrData := 0
      switch(io.memRw.addr(1 downto 0)) {
        is(0) {
          io.dataBus.wrData(7 downto 0) := tmpWrData
          io.dataBus.be := B"4'b0001"
        }
        is(1) {
          io.dataBus.wrData(15 downto 8) := tmpWrData
          io.dataBus.be := B"4'b0010"
        }
        is(2) {
          io.dataBus.wrData(23 downto 16) := tmpWrData
          io.dataBus.be := B"4'b0100"
        }
        is(3) {
          io.dataBus.wrData(31 downto 24) := tmpWrData
          io.dataBus.be := B"4'b1000"
        }
      }
      when(io.memRw.isSignExtend) {
        io.memRdData := tmpData.asSInt.resize(32 bits).asUInt
      }.otherwise {
        io.memRdData := tmpData.resized
      }
    }
  }

}
