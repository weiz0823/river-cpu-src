package sim

object IType {
  def apply(opcode: Int, funct3: Int, rd: Int, rs: Int, imm: Int): Long = {
    var result: Long = 0
    result |= opcode & 0x7f
    result |= rd << 7
    result |= funct3 << 12
    result |= rs << 15
    result |= imm.toLong << 20
    result &= 0xffffffffL
    result
  }
}
object RType {
  def apply(
      opcode: Int,
      funct3: Int,
      rd: Int,
      rs1: Int,
      rs2: Int,
      funct7: Int
  ): Long = {
    var result: Long = 0
    result |= opcode & 0x7f
    result |= rd << 7
    result |= funct3 << 12
    result |= rs1 << 15
    result |= rs2 << 20
    result |= funct7.toLong << 25
    result &= 0xffffffffL
    result
  }
}

object ADDI {
  def apply(rd: Int, rs: Int, imm: Int): Long = IType(0x13, 0, rd, rs, imm)
}
object SLLI {
  def apply(rd: Int, rs: Int, shamt: Int): Long =
    RType(0x13, 1, rd, rs, shamt, 0)
}

class DataProvider {
  var baseAddr: Long = 0
  var dataset: Vector[Long] = Vector[Long]()

  def isValidAddr(addr: Long) = {
    val idx = (addr - baseAddr).toInt / 4
    0 <= idx && idx < dataset.length
  }

  def get(addr: Long): Long = {
    val idx = (addr - baseAddr).toInt / 4
    if (0 <= idx && idx < dataset.length) {
      dataset(idx)
    } else {
      0
    }
  }

  def set(addr: Long, data: Long, byteEnable: Int) {
    val idx = (addr - baseAddr).toInt / 4
    var mask: Long = 0
    if ((byteEnable & 1) != 0) mask |= 0xffL
    if ((byteEnable & 2) != 0) mask |= 0xffL << 8
    if ((byteEnable & 4) != 0) mask |= 0xffL << 16
    if ((byteEnable & 8) != 0) mask |= 0xffL << 24
    if (0 <= idx && idx < dataset.length) {
      var word = dataset(idx)
      word &= ~mask
      word |= data & mask
      dataset = dataset.updated(idx, word)
    }
  }

  def setWord(addr: Long, data: Long) {
    val idx = (addr - baseAddr).toInt / 4
    if (0 <= idx && idx < dataset.length) {
      dataset = dataset.updated(idx, data)
    }
  }

  def setByte(addr: Long, data: Long) {
    val idx = (addr - baseAddr).toInt / 4
    val offset = (addr - baseAddr).toInt - idx * 4
    var word = dataset(idx)
    word &= ~(0xffL << (offset * 8))
    word |= (data & 0xffL) << (offset * 8)
    if (0 <= idx && idx < dataset.length) {
      dataset = dataset.updated(idx, word)
    }
  }

  def setHalf(addr: Long, data: Long) {
    val idx = (addr - baseAddr).toInt / 4
    val offset = (addr - baseAddr).toInt - idx * 4
    var word = dataset(idx)
    word &= ~(0xffffL << (offset * 8))
    word |= (data & 0xffffL) << (offset * 8)
    if (0 <= idx && idx < dataset.length) {
      dataset = dataset.updated(idx, word)
    }
  }
}

object DataProvider {
  def apply(baseAddr: Long, dataset: Vector[Long]): DataProvider = {
    val p = new DataProvider
    p.baseAddr = baseAddr
    p.dataset = dataset
    p
  }
}

object genInst {
  def ADDI(rd: Int, rs: Int, imm: Int): Long =
    IType(0x13, 0, rd, rs, imm)
  def SLTI(rd: Int, rs: Int, imm: Int): Long =
    IType(0x13, 2, rd, rs, imm)
  def SLTIU(rd: Int, rs: Int, imm: Int): Long =
    IType(0x13, 3, rd, rs, imm)
  def XORI(rd: Int, rs: Int, imm: Int): Long =
    IType(0x13, 4, rd, rs, imm)
  def ORI(rd: Int, rs: Int, imm: Int): Long =
    IType(0x13, 6, rd, rs, imm)
  def ANDI(rd: Int, rs: Int, imm: Int): Long =
    IType(0x13, 7, rd, rs, imm)
  def SLLI(rd: Int, rs: Int, shamt: Int): Long =
    RType(0x13, 1, rd, rs, shamt, 0)
  def SRLI(rd: Int, rs: Int, shamt: Int): Long =
    RType(0x13, 5, rd, rs, shamt, 0)
  def SRAI(rd: Int, rs: Int, shamt: Int): Long =
    RType(0x13, 5, rd, rs, shamt, 0x20)
}
