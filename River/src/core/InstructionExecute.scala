package core

import spinal.core._
import model._
import spinal.lib.master
import core.csr.CsrWrTypes
import core.csr.PrivilegeEnum

/** EX stage.
  */
class InstructionExecute extends Component {
  val io = new Bundle {
    val id = in(IDBundle())
    val ex = out(EXBundle())

    val csrBus = master(CsrBusBundle())
    val except = out(ExceptBundle())
    val trapRet = out(TrapRetBundle())
    val exStall = in(Bool())
    val flushCurrent = in(Bool())
    val privMode = in(PrivilegeEnum())
  }

  val alu = new Alu
  alu.io.inp := io.id.aluInput

  val except = ExceptBundle()
  except.e := False
  except.code := ExceptCode.ILLEGAL_INST
  except.assistVal := io.id.inst.asUInt

  io.csrBus.addr := io.id.aluInput.val2(11 downto 0) // from imm
  io.csrBus.wrData := io.id.aluInput.val1
  io.csrBus.wrType := io.id.instFlush ? CsrWrTypes.NONE | io.id.csrWrType
  io.trapRet.e := ~io.id.instFlush & io.id.trapRet.e
  io.trapRet.priv := io.id.trapRet.priv
  when(
    io.id.trapRet.e &
      (io.id.trapRet.priv.asBits.asUInt > io.privMode.asBits.asUInt)
  ) {
    except.makeIllegalInstruction(io.id.inst)
  }

  io.ex.regWb := io.id.regWb
  io.ex.memRw := io.id.memRw
  when(io.id.instFlush) {
    io.ex.regWb.disable()
    io.ex.memRw.disable()
  }

  switch(io.id.calcType) {
    is(AluCalcType.ARITH) {
      io.ex.regWb.data := alu.io.result
      io.ex.regWb.valid := True
    }
    is(AluCalcType.ADDR) {
      io.ex.memRw.addr := alu.io.result
    }
  }

  when(io.id.csrWrType =/= CsrWrTypes.NONE) {
    io.ex.regWb.data := io.csrBus.rdData
    io.ex.regWb.valid := True
    when(~io.csrBus.instValid) {
      except.makeIllegalInstruction(io.id.inst)
    }
  }

  io.except.e := ~io.id.instFlush & (io.id.except.e | except.e)
  io.except.code := Mux(io.id.except.e, io.id.except.code, except.code)
  io.except.assistVal := Mux(
    io.id.except.e,
    io.id.except.assistVal,
    except.assistVal
  )

  // registers
  val regFlushCurrent = RegInit(False)
  when(io.exStall & io.flushCurrent) {
    regFlushCurrent := True
  }
  when(~io.exStall) {
    regFlushCurrent := False
  }
  io.ex.instFlush := io.id.instFlush | io.flushCurrent | regFlushCurrent
}
