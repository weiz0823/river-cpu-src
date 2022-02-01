package core.mmu

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

import model._

class PageTableWalker extends Component {
  val io = new Bundle {
    val e = in Bool ()
    val vpn = in UInt (20 bits)
    val basePPN = in UInt (22 bits) // physical PDT base from CSR.satp
    val abort = in Bool ()

    val ack = out Bool ()
    val fault = out Bool ()
    val tlbReq = master(TLBRequest())
    val memBus = master(ReadonlyBusBundle(32 bits))
  }

  io.memBus.stb := False

  io.ack := False
  io.fault := False

  val vpn = Reg(UInt(20 bits))
  val basePPN = Reg(UInt(22 bits))
  val pte = Reg(UInt(32 bits))

  val memAck = io.memBus.ack
  val data = io.memBus.data

  def makePteForSuperpage(vpn: UInt, oldPte: UInt): Bits = {
    oldPte(31 downto 20) ## vpn(9 downto 0) ## oldPte(9 downto 0)
  }

  val fsm = new StateMachine {
    val IDLE: State = new State with EntryPoint {
      whenIsActive {
        when(io.e && !io.abort) {
          vpn := io.vpn
          basePPN := io.basePPN
          goto(FETCH_PDE)
        }
      }
    }

    val FETCH_PDE: State = new State {
      whenIsActive {
        // val pdeAddr = base(33 downto 12) @@ vpn(19 downto 10) @@ U(0, 2 bits)
        // io.memBus.addr := pdeAddr.resized
        io.memBus.stb := True

        when(memAck) {
          val e = PTE()
          pte := data
          e.assignFromBits(data.asBits)

          // io.memBus.stb := False
          when(!e.V || (!e.R && e.W)) { // invalid PDE
            io.ack := True
            io.fault := True
            goto(IDLE)
          } elsewhen (e.R || e.X) { // superpage
            io.ack := True
            goto(IDLE)
            when(e.ppn(9 downto 0) =/= 0) { // superpage misalign
              io.fault := True
            } otherwise {
              // NOTE: superpage is split into normal pages in TLB
              io.tlbReq.op := TLBOp.INSERT
            }
          } otherwise {
            goto(FETCH_PTE)
          }
        }

        // no abort support
        /*when(io.abort) {
          io.memBus.stb := False
          goto(IDLE)
        }*/
      }
    }

    val FETCH_PTE: State = new State {
      whenIsActive {
        // val pteAddr = pte(31 downto 10) @@ vpn(9 downto 0) @@ U(0, 2 bits)
        // io.memBus.addr := pteAddr.resized
        io.memBus.stb := True

        when(memAck) {
          val e = PTE()
          e.assignFromBits(data.asBits)

          // io.memBus.stb := False
          io.ack := True
          goto(IDLE)
          when(!e.V || (!e.R && e.W)) { // invalid PTE
            io.fault := True
          } elsewhen (e.R || e.X) { // normal page
            io.tlbReq.op := TLBOp.INSERT
          } otherwise { // see priviledged spec 4.3.2
            // raise fault directly, do not put this PTE (RWX=0) into TLB
            io.fault := True
          }
        }

        // no abort support
        /*when(io.abort) {
          io.memBus.stb := False
          goto(IDLE)
        }*/
      }
    }
  }

  io.memBus.addr :=
    Mux(
      fsm.isActive(fsm.FETCH_PDE),
      basePPN(19 downto 0) @@ vpn(19 downto 10) @@ U(0, 2 bits),
      pte(29 downto 10) @@ vpn(9 downto 0) @@ U(0, 2 bits)
    )

  io.tlbReq.op := TLBOp.NOP
  io.tlbReq.vpn := vpn
  io.tlbReq.pte := Mux(
    fsm.isActive(fsm.FETCH_PDE),
    makePteForSuperpage(vpn, data),
    data.asBits
  )
}
