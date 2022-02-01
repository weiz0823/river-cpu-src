package model

import spinal.core._
import spinal.lib.IMasterSlave

/** Readonly bus.
  *
  * Used for TLB refill from page table walker, and instruction dataflow.
  */
final case class ReadonlyBusBundle(addrWidth: BitCount)
    extends Bundle
    with IMasterSlave {
  val stb = Bool()
  val ack = Bool()
  val addr = UInt(addrWidth)
  val data = UInt(32 bits)

  override def asMaster(): Unit = {
    out(stb, addr)
    in(ack, data)
  }
}
