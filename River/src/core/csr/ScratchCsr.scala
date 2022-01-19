package core.csr

import spinal.core._
import spinal.lib.slave
import model._

/** Holds mscratch, sscratch.
  */
class ScratchCsr extends Component {
  val io = new Bundle {
    val scratchCsr = slave(CsrRWBundle())
  }

  val mWrCtrl = new CsrWriteControl
  val sWrCtrl = new CsrWriteControl

  val mscratch = Reg(UInt(32 bits))
  val sscratch = Reg(UInt(32 bits))

  mWrCtrl.io.current := mscratch
  mWrCtrl.io.wrData := io.scratchCsr.wrData
  mWrCtrl.io.wrType := io.scratchCsr.wrType

  sWrCtrl.io.current := sscratch
  sWrCtrl.io.wrData := io.scratchCsr.wrData
  sWrCtrl.io.wrType := io.scratchCsr.wrType

  io.scratchCsr.rdData := io.scratchCsr.priv.mux(
    PrivilegeEnum.M -> mscratch,
    default -> sscratch
  )
  io.scratchCsr.instValid := (io.scratchCsr.priv === PrivilegeEnum.M) || (io.scratchCsr.priv === PrivilegeEnum.S)
  when(mWrCtrl.io.e && io.scratchCsr.priv === PrivilegeEnum.M) {
    mscratch := mWrCtrl.io.next
  }
  when(sWrCtrl.io.e && io.scratchCsr.priv === PrivilegeEnum.S) {
    sscratch := sWrCtrl.io.next
  }
}
