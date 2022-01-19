package model

import spinal.core._
import spinal.lib.slave
import spinal.lib.IMasterSlave
import spinal.lib.master

final case class TrapCsrBundle() extends Bundle with IMasterSlave {
  val status = CsrRWBundle()
  val trapVec = CsrRWBundle()
  val eDeleg = CsrRWBundle()
  val iDeleg = CsrRWBundle()
  val ip = CsrRWBundle()
  val ie = CsrRWBundle()
  val epc = CsrRWBundle()
  val cause = CsrRWBundle()
  val trapVal = CsrRWBundle()

  def asMaster(): Unit = {
    master(status, trapVec, eDeleg, iDeleg, ip, ie, epc, cause, trapVal)
  }
}
