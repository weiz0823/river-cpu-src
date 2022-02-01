package sim

import spinal.core.sim._
import core.mmu._

object TLBSim {
  def main(args: Array[String]) {
    SpinalSimConfig().withWave.compile(new TLB(4, 4)).doSim {
      dut =>
        dut.clockDomain.forkStimulus(period = 2)
        dut.clockDomain.waitRisingEdge()

        dut.io.query.vpn #= 63

        dut.io.req.op #= TLBOp.NOP
        dut.io.req.vpn #= 1
        sleep(2)

        dut.io.req.op #= TLBOp.INSERT
        dut.io.req.vpn #= 63
        dut.io.req.pte #= 0x7fffffff
        sleep(4)

        dut.io.req.op #= TLBOp.NOP
        sleep(4)

        dut.io.req.op #= TLBOp.INSERT
        dut.io.req.vpn #= 62
        dut.io.req.pte #= 0x7eadbeef
        sleep(4)

        dut.io.req.op #= TLBOp.INVALIDATE
        dut.io.req.vpn #= 63
        dut.clockDomain.waitRisingEdge()
        sleep(2)

        dut.io.req.op #= TLBOp.NOP
        sleep(2)
    }
  }
}