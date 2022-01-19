package model

import spinal.core._

final case class BranchPredictBundle() extends Bundle {
  val take = Bool()
  val addr = UInt(32 bits)
}
