package sim

import spinal.core._
import spinal.lib._
import config._
import model._
import core._
import peripheral._

class CoreSimTop(config: CoreConfig, csrConfig: CsrConfig) extends Component {
  val io = new Bundle {
    val instBus = master(InternalBusBundle())
    val dataBus = master(InternalBusBundle())

  }

  val coreTop = new CoreTop(config, csrConfig)

  val coreLocalInt = new CoreLocalInt()

  coreTop.io.softInt := coreLocalInt.io.softInt
  coreTop.io.timeInt := coreLocalInt.io.timeInt
  coreTop.io.timeCsrData := coreLocalInt.io.timeCsrData
  coreTop.io.extInt := False

}