package config

case class CsrConfig(
    // RV32I with machine/supervisor/user privilege mode
    val isa: Long = csrEncoder.encodeISA("RV32I", "MSU"),
    val initStatus: Long = 0
) {
  val counterAddr: AddressConfig = AddressConfig(0xc00, 5)
}

object csrEncoder {
  def encodeISA(s: String, privMode: String): Long = {
    var ext = 0
    var mxl = 0
    if (s.startsWith("RV32")) {
      mxl = 1
      for (c <- s.substring(4)) {
        if (c >= 'A' && c <= 'Z') {
          ext |= 1 << (c - 'A')
        }
      }
    }
    for (c <- privMode) {
      if (c == 'U' || c == 'S') {
        ext |= 1 << (c - 'A')
      }
    }
    if (mxl == 1) {
      (mxl.toLong << 30) | ext
    } else {
      0.toLong
    }
  }
}
