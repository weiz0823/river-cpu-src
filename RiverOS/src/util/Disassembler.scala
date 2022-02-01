package util

import spinal.core._
import spinal.lib._
import java.io.PrintStream

object Disassembler {
  def main(args: Array[String]) {
    println(
      disassemble(Integer.parseUnsignedInt(args(0).substring(2), 16), Integer.parseUnsignedInt(args(1).substring(2), 16))
    )
  }

  def getRegisterString(reg: Int): String = {
    reg match {
      case 0 =>
        "zero"
      case 1                         => "ra"
      case 2                         => "sp"
      case 3                         => "gp"
      case 4                         => "tp"
      case i if (i >= 5 && i <= 7)   => s"t${reg - 5}"
      case 8                         => "s0"
      case 9                         => "s1"
      case i if (i >= 10 && i <= 17) => s"a${reg - 10}"
      case i if (i >= 18 && i <= 27) => s"s${reg - 16}"
      case i if (i >= 28 && i <= 31) => s"t${reg - 25}"
      case _                         => "?"
    }
  }

  private def signExtend(value: Int, nBits: Int): Int = {
    var v = value
    if ((v & (1 << nBits)) != 0) { // I hope it works cuz i don't have binaries to test this
      v = -(-v & ((1 << nBits) - 1))
    }
    v
  }

  private def getOffsetForBType(inst: Int): Int = {
    val offset = (((inst >>> 8) & ((1 << 4) - 1)) << 1) |
      (((inst >>> 25) & ((1 << 6) - 1)) << 5) |
      (((inst >>> 7) & 1) << 11) |
      (((inst >>> 31) & 1) << 12) // probably has a bug - did not test
    signExtend(offset, 12)
  }

  private def getOffsetForJType(inst: Int): Int = {
    val imm = inst >> 12;
    val offset = (((imm >>> 9) & ((1 << 10) - 1)) << 1) |
      (((imm >>> 8) & 1) << 11) |
      ((imm & ((1 << 8) - 1)) << 12) |
      (((imm >>> 19) & 1) << 20);
    signExtend(offset, 20);
  }

  def disassemble(instruction: Int, virtualAddress: Int): String = {
    val opcode = instruction & ((1 << 7) - 1)
    val rd = instruction >> 7 & ((1 << 5) - 1)
    val funct3 = instruction >> 12 & ((1 << 3) - 1)
    val rs1 = instruction >> 15 & ((1 << 5) - 1)
    val rs2 = instruction >> 20 & ((1 << 5) - 1)
    var imm110 = instruction >> 20 & ((1 << 12) - 1)
    val funct7 = instruction >> 25
    if (opcode == 55) { // LUI
      s"lui ${getRegisterString(rd)}, ${Integer.toUnsignedLong((instruction >>> 12) << 12).toHexString}"
    } else if (opcode == 23) { // AUIPC
      s"auipc ${getRegisterString(rd)}, ${Integer.toUnsignedLong((instruction >>> 12) << 12).toHexString}"
    } else if (opcode == 111) { // JAL
      val offset = getOffsetForJType(instruction);
      val jumpTo = virtualAddress + offset;
      s"jal ${getRegisterString(rd)}, ${offset.toHexString}(=> ${jumpTo.toHexString})"
    } else if (opcode == 103 && funct3 == 0) { // jalr
      imm110 = signExtend(imm110, 11);
      s"jalr ${getRegisterString(rd)}, ${imm110.toHexString}(${getRegisterString(rs1)})"
    } else if (opcode == 99) { // B-type
      val offset = getOffsetForBType(instruction);
      val instr =
        Array("beq", "bne", "??", "??", "blt", "bge", "bltu", "bgeu").apply(
          funct3
        );
      val jumpTo = virtualAddress + offset;
      s"$instr ${getRegisterString(rs1)}, ${getRegisterString(rs2)}, ${offset.toHexString}(=> ${jumpTo.toHexString})"
    } else if (opcode == 3) { // I-type - LB, LH, LW, LBU, LHU
      val instr =
        Array("lb", "lh", "lw", "??", "lbu", "lhu", "??", "??").apply(funct3);
      s"$instr ${getRegisterString(rd)}, ${signExtend(imm110, 11).toHexString}(${getRegisterString(rs1)})"
    } else if (opcode == 35) { // S-type SB, SH, SW
      val instr =
        Array("sb", "sh", "sw", "??", "??", "??", "??", "??").apply(funct3);
      val imm = rd | ((imm110 >>> 5) << 5);
      s"$instr ${getRegisterString(rs2)}, ${signExtend(imm, 11).toHexString}(${getRegisterString(rs1)})"
    } else if (opcode == 19) {
      if (funct3 == 1) { // SLLI
        s"slli ${getRegisterString(rd)}, ${getRegisterString(rs1)}, ${imm110.toHexString}"
      } else if (funct3 == 5) {
        if (funct7 == 32) { // SRAI
          s"srai ${getRegisterString(rd)}, ${getRegisterString(rs1)}, ${(imm110 & ((1 << 5) - 1)).toHexString}"
        } else { // SRLI
          s"srli ${getRegisterString(rd)}, ${getRegisterString(rs1)}, ${imm110.toHexString}"
        }
      } else { // I-type - ADDI, SLTI, SLTIU, XORI, ORI, ANDI
        val instr =
          Array("addi", "??", "slti", "sltiu", "xori", "??", "ori", "andi")
            .apply(funct3);
        if (instr.equals("addi") || instr.equals("slti")) { // sign-extend
          imm110 = signExtend(imm110, 11);
        }
        s"$instr ${getRegisterString(rd)}, ${getRegisterString(rs1)}, ${imm110.toHexString}"
      }
    } else if (opcode == 51) { // R-type
      if (funct7 == 32) { // SUB, SRA
        val instr =
          Array("sub", "??", "??", "??", "??", "sra", "??", "??").apply(funct3)
        s"$instr ${getRegisterString(rd)}, ${getRegisterString(rs1)}, ${getRegisterString(rs2)}"
      } else if (funct7 == 0) {
        val instr =
          Array("add", "sll", "slt", "sltu", "xor", "srl", "or", "and").apply(
            funct3
          );
        s"$instr ${getRegisterString(rd)}, ${getRegisterString(rs1)}, ${getRegisterString(rs2)}"
      } else if (funct7 == 1) {
        val instr =
          Array("mul", "mulh", "mulhsu", "mulhu", "div", "divu", "rem", "remu")
            .apply(funct3)
        s"$instr ${getRegisterString(rd)}, ${getRegisterString(rs1)}, ${getRegisterString(rs2)}"
      } else {
        ""
      }
    } else if (opcode == 15) {
      if (funct3 == 1) { // FENCE.I
        "fence.i"
      } else { // FENCE
        s"fence ${(imm110 >>> 4 << 4).toHexString}, ${(imm110 & ((1 << 4) - 1)).toHexString}"
      }
    } else if (opcode == 115) {
      if (funct3 == 0) {
        if (imm110 == 0) { // ECALL
          "ecall"
        } else if (imm110 == 1) { // EBREAK
          "ebreak"
        } else {
          s"????(${instruction})"
        }
      } else {
        val instr = Array(
          "",
          "csrrw",
          "csrrs",
          "csrrc",
          "??",
          "csrrwi",
          "csrrsi",
          "csrrci"
        ).apply(funct3)
        s"$instr ${getRegisterString(rd)}, ${imm110.toHexString}, ${getRegisterString(rs1)}"
      }
    } else {
      s"????($instruction)"
    }
  }
}
