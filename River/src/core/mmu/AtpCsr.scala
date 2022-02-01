package core.mmu

import spinal.core._
import spinal.lib.slave
import model._
import core.csr.CsrWriteControl
import core.csr.PrivilegeEnum

/** Holds satp.
  */
class AtpCsr extends Component {
  val io = new Bundle {
    val atpCsr = slave(CsrRWBundle())
    val satp = out(UInt(32 bits))
  }

  val sWrCtrl = new CsrWriteControl

  val satp = RegInit(U(0, 32 bits))

  sWrCtrl.io.current := satp
  sWrCtrl.io.wrData := io.atpCsr.wrData
  sWrCtrl.io.wrType := io.atpCsr.wrType

  io.atpCsr.rdData := satp
  io.satp := satp
  io.atpCsr.instValid := (io.atpCsr.priv === PrivilegeEnum.S)
  when(sWrCtrl.io.e && io.atpCsr.priv === PrivilegeEnum.S) {
    satp := sWrCtrl.io.next
  }
}
