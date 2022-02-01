package util

class CoreLocalIntModel(val baseAddr: Long) {
  private var softInt = false
  private var time: Long = 0
  private var timeCmp: Long = 1L << 32
  private var counter = 0

  private def byteMask(byteEnable: Int): Long = {
    var mask: Long = 0
    if ((byteEnable & 1) != 0) mask |= 0xffL
    if ((byteEnable & 2) != 0) mask |= 0xffL << 8
    if ((byteEnable & 4) != 0) mask |= 0xffL << 16
    if ((byteEnable & 8) != 0) mask |= 0xffL << 24
    mask
  }

  def set(addr: Long, byteEnable: Int, value: Long): Unit = {
    val mask = byteMask(byteEnable)
    (addr - baseAddr) match {
      case 0 => {
        if ((value & mask & 1) != 0) {
          softInt = true
        }
      }
      case 0x4000 => timeCmp = (timeCmp & ~mask) | (value & mask)
      case 0x4004 =>
        timeCmp = (timeCmp & ~(mask << 32)) | ((value & mask) << 32)
      case _ => {}
    }
  }

  def get(addr: Long, byteEnable: Int): Long = {
    val mask = byteMask(byteEnable)
    (addr - baseAddr) match {
      case 0      => if (softInt) 1 else 0
      case 0x4000 => timeCmp & 0xffffffffL
      case 0x4004 => timeCmp >> 32
      case 0xbff8 => time & 0xffffffffL
      case 0xbffc => time >> 32
      case _      => 0
    }
  }

  def isValid(addr: Long): Boolean = {
    baseAddr <= addr && addr < baseAddr + 0x10000
  }

  // should be called every clock cycle
  def clockTick() {
    if (counter == 4) {
      counter = 0
      time += 1
    } else {
      counter += 1
    }
  }

  def hasSoftInt() = softInt
  def hasTimerInt() = time >= timeCmp
  def getTime() = time
}
