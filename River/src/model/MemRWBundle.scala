package model

import spinal.core._

/** Memory R/W request data model.
  */
final case class MemRWBundle() extends Bundle {
  val ce, we = Bool()
  val addr = UInt(32 bits)
  val len = MemRWLength()
  val isSignExtend = Bool()
  // read or write data based on r/w type
  val data = UInt(32 bits)

  def disable() {
    ce := False
  }
}

object MemRWLength extends SpinalEnum {
  val WORD, HALF, BYTE = newElement()
  defaultEncoding = SpinalEnumEncoding("staticEncoding")(
    BYTE -> 0,
    HALF -> 1,
    WORD -> 2
  )
}
