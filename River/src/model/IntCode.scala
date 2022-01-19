package model

import spinal.core._

object IntCode extends SpinalEnum {
  val SOFT, TIME, EXT = newElement()

  defaultEncoding = SpinalEnumEncoding("staticEncoding")(
    SOFT -> 0,
    TIME -> 1,
    EXT -> 2
  )
}
