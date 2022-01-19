package model

import spinal.core._

object ExceptCode extends SpinalEnum {
  val INST_MISALIGN = newElement()
  val INST_FAULT = newElement()
  val ILLEGAL_INST = newElement()
  val BREAKPOINT = newElement()
  val LOAD_MISALIGN = newElement()
  val LOAD_FAULT = newElement()
  val STORE_MISALIGN = newElement()
  val STORE_FAULT = newElement()
  // 4-aligned
  val ECALL_U, ECALL_S, ECALL_H, ECALL_M = newElement()
  val INST_PAGE, LOAD_PAGE, STORE_PAGE = newElement()

  defaultEncoding = SpinalEnumEncoding("staticEncoding")(
    INST_MISALIGN -> 0,
    INST_FAULT -> 1,
    ILLEGAL_INST -> 2,
    BREAKPOINT -> 3,
    LOAD_MISALIGN -> 4,
    LOAD_FAULT -> 5,
    STORE_MISALIGN -> 6,
    STORE_FAULT -> 7,
    ECALL_U -> 8,
    ECALL_S -> 9,
    ECALL_H -> 10,
    ECALL_M -> 11,
    INST_PAGE -> 12,
    LOAD_PAGE -> 13,
    STORE_PAGE -> 15
  )
}
