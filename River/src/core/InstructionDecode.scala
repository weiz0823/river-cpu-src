package core

import spinal.core._
import spinal.lib.master
import model._
import core.csr.CsrWrTypes
import core.csr.PrivilegeEnum
import core.mmu.TLBOp

/** Decode instruction.
  */
class InstructionDecode extends Component {
  val io = new Bundle {
    val iF = in(IFBundle())
    val id = out(IDBundle())

    // to RegisterFile
    val regFileRd1 = master(RegisterReadBundle())
    val regFileRd2 = master(RegisterReadBundle())

    val branch = out(BranchBundle())
    val idStall = in(Bool())
    val flushCurrent = in(Bool())
    val privMode = in(PrivilegeEnum())
  }

  val brDecide = new BranchDecide
  val immGen = new ImmediateGenerate

  // registers
  val regFlushCurrent = RegInit(False)
  when(io.idStall & io.flushCurrent) {
    regFlushCurrent := True
  }
  when(~io.idStall) {
    regFlushCurrent := False
  }
  io.id.instFlush := io.iF.instFlush | io.flushCurrent | regFlushCurrent

  // potential fields
  val opcode = io.iF.inst(6 downto 0);
  val rd = io.iF.inst(11 downto 7) asUInt
  val funct3 = io.iF.inst(14 downto 12)
  val rs1 = io.iF.inst(19 downto 15) asUInt
  val rs2 = io.iF.inst(24 downto 20) asUInt
  val funct7 = io.iF.inst(31 downto 25)

  val instValid = Bool()
  val except = ExceptBundle()
  except.e := False
  except.code := ExceptCode.ILLEGAL_INST
  except.assistVal := 0

  // default values
  io.regFileRd1.addr := rs1
  io.regFileRd2.addr := rs2
  io.regFileRd1.e := False
  io.regFileRd2.e := False
  // instValid := True
  io.id.regWb.dest := rd
  io.id.regWb.e := False
  io.id.regWb.data := 0
  io.id.regWb.valid := False
  io.id.aluInput.val1 := io.regFileRd1.data
  io.id.aluInput.val2 := immGen.io.imm
  io.id.calcType := AluCalcType.DISABLE
  io.id.aluInput.op := AluOp.ADD
  io.id.memRw.addr := 0
  io.id.memRw.data := io.regFileRd2.data
  io.id.memRw.ce := False
  io.id.memRw.we := opcode(5)
  io.id.memRw.isSignExtend := ~funct3(2)
  // io.id.memRw.len := MemRWLength.WORD
  io.id.memRw.len.assignFromBits(funct3(1 downto 0))
  io.id.csrWrType := CsrWrTypes.NONE
  io.id.pc := io.iF.pc
  io.id.inst := io.iF.inst
  io.id.except := Mux(io.iF.except.e, io.iF.except, except)
  io.id.trapRet.e := False
  io.id.trapRet.priv.assignFromBits(funct7(4 downto 3))
  io.id.tlbReqOp := TLBOp.NOP

  // invalidate branch on instruction flush
  val branchPredictIncorrect = (brDecide.io.branch.e ^ io.iF.branchPredict.take)
  io.branch.e := ~io.idStall & ~io.iF.instFlush & branchPredictIncorrect
  io.branch.addr := Mux(
    brDecide.io.branch.e,
    brDecide.io.branch.addr,
    io.iF.pc + 4
  )
  brDecide.io.inp.op := BranchOp.ALWAYS
  brDecide.io.inp.neg := True // disable branch
  brDecide.io.inp.val1 := io.regFileRd1.data
  brDecide.io.inp.val2 := io.regFileRd2.data
  brDecide.io.base := io.iF.pc
  brDecide.io.offset := immGen.io.imm

  immGen.io.inst := io.iF.inst.asUInt
  immGen.io.immType := ImmType.R

  when(~instValid) {
    // illegal instruction
    except.makeIllegalInstruction(io.iF.inst)
  } elsewhen(~io.iF.instFlush & brDecide.io.branch.e & brDecide.io.branch.addr(1)){
    except.e := True
    except.code := ExceptCode.INST_MISALIGN
    except.assistVal := brDecide.io.branch.addr
  }

  switch(opcode) {
    is(M"0-10011") {
      // arithmetic/logic
      io.id.calcType := AluCalcType.ARITH
      io.id.aluInput.val1 := io.regFileRd1.data
      io.regFileRd1.e := True
      io.id.regWb.e := True
      immGen.io.immType := ImmType.I
      when(opcode(5)) {
        // with register
        io.id.aluInput.val2 := io.regFileRd2.data
        io.regFileRd2.e := True
        io.id.aluInput.op.assignFromBits(funct7(5) ## funct3)
        instValid := funct3
          .resize(4 bits)
          .mux(
            AluOp.ADD.asBits -> (funct7 === M"0-00000"),
            AluOp.SRL.asBits -> (funct7 === M"0-00000"),
            default -> (funct7 === 0)
          )
      }.otherwise {
        // with imm
        io.id.aluInput.val2 := immGen.io.imm
        when(funct3 === AluOp.SRL.asBits(2 downto 0)) {
          io.id.aluInput.op.assignFromBits(funct7(5) ## funct3)
        }.otherwise {
          io.id.aluInput.op.assignFromBits(False ## funct3)
        }
        instValid := funct3
          .resize(4 bits)
          .mux(
            AluOp.SLL.asBits -> (funct7 === 0),
            AluOp.SRL.asBits -> (funct7 === M"0-00000"),
            default -> True
          )
      }
    }
    is(M"0-10111") {
      // lui/auipc
      immGen.io.immType := ImmType.U
      instValid := True
      io.id.calcType := AluCalcType.ARITH
      io.id.aluInput.op := AluOp.ADD
      io.id.aluInput.val2 := immGen.io.imm
      io.id.regWb.e := True
      when(opcode(5)) {
        // lui
        io.id.aluInput.val1 := 0
      }.otherwise {
        // auipc
        io.id.aluInput.val1 := io.iF.pc
      }
    }
    is(M"0-00011") {
      // load/store
      instValid := True
      io.id.memRw.ce := True
      // addr = rs1 + offset
      io.id.aluInput.val1 := io.regFileRd1.data
      io.regFileRd1.e := True
      io.id.aluInput.val2 := immGen.io.imm
      io.id.calcType := AluCalcType.ADDR
      io.id.aluInput.op := AluOp.ADD
      when(opcode(5)) {
        // store
        immGen.io.immType := ImmType.S
        // io.id.memRw.we := True
        // io.id.memRw.data := io.regFileRd2.data
        io.regFileRd2.e := True
        // store cannot be 1--
        when(funct3(2)) { instValid := False }
      }.otherwise {
        // load
        immGen.io.immType := ImmType.I
        // io.id.memRw.we := False
        // io.id.memRw.isSignExtend := ~funct3(2)
        io.id.regWb.e := True
      }
      // io.id.memRw.len.assignFromBits(funct3(1 downto 0))
      when(funct3(1 downto 0) === B"2'b11") {
        instValid := False
      }
    }
    is(B"7'b1100011") {
      // branch
      io.regFileRd1.e := True
      io.regFileRd2.e := True
      immGen.io.immType := ImmType.B
      instValid := funct3(2 downto 1) =/= BranchOp.ALWAYS.asBits
      brDecide.io.base := io.iF.pc
      brDecide.io.inp.op.assignFromBits(funct3(2 downto 1))
      brDecide.io.inp.neg := funct3(0)
    }
    is(M"110-111") {
      // jalr/jal
      brDecide.io.inp.op := BranchOp.ALWAYS
      brDecide.io.inp.neg := False
      io.id.regWb.e := True
      io.id.calcType := AluCalcType.ARITH
      io.id.aluInput.op := AluOp.ADD
      io.id.aluInput.val1 := io.iF.pc
      // link back to pc+4
      io.id.aluInput.val2 := 4
      when(opcode(3)) {
        // jal
        immGen.io.immType := ImmType.J
        instValid := True
        brDecide.io.base := io.iF.pc
      }.otherwise {
        // jalr
        immGen.io.immType := ImmType.I
        instValid := funct3 === 0
        brDecide.io.base := io.regFileRd1.data
        io.regFileRd1.e := True
      }
    }
    is(B"1110011") {
      // SYSTEM
      when(funct3(1 downto 0) =/= 0) {
        // csr
        instValid := True
        immGen.io.immType := ImmType.I // because we need imm as address of csr
        io.id.calcType := AluCalcType.DISABLE
        io.id.aluInput.val2 := immGen.io.imm
        io.id.regWb.e := True
        // because we defined encoding
        io.id.csrWrType.assignFromBits(funct3(1 downto 0))
        when(funct3(2)) {
          // csrr_i
          io.id.aluInput.val1 := rs1.resized
        }.otherwise {
          // csrr_
          io.id.aluInput.val1 := io.regFileRd1.data
          io.regFileRd1.e := True
        }
      }.otherwise {
        switch(funct7(2 downto 0)) {
          is(0) {
            switch(rs2) {
              is(0) {
                // ecall
                instValid :=
                  (io.iF.inst(19 downto 7) === 0) & (funct7 === 0)
                except.e := True
                except.code.assignFromBits(B(2, 2 bits) ## io.privMode.asBits)
              }
              is(1) {
                // ebreak
                instValid :=
                  (io.iF.inst(19 downto 7) === 0) & (funct7 === 0)
                except.e := True
                except.code := ExceptCode.BREAKPOINT
                except.assistVal := io.iF.pc
              }
              is(2) {
                // _ret
                instValid :=
                  (io.iF.inst(19 downto 7) === 0) &
                    (funct7(6 downto 5) === 0)
                io.id.trapRet.e := True
              }
              default {
                instValid := False
              }
            }
          }
          is(1) {
            // sfence.vma
            instValid :=
              (io.iF.inst(14 downto 7) === 0) & (funct7 === B"7'b0001001")
            // ignore rs1 and rs2, we always invalidate all
            io.id.tlbReqOp := TLBOp.INVALIDATE_ALL
          }
          default {
            instValid := False
          }
        }
      }
    }
    is(B"0001111") {
      // fence, implemented as nop
      instValid := funct3 === 0
    }
    default {
      instValid := False
    }
  }
}
