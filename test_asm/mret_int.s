.equ KERNEL_STACK_INIT, 0x80800000
.equ USER_STACK_INIT, 0x807F0000
.equ MSTATUS_MPP_MASK, 0x1800
.equ SYS_exit, 1
.equ EX_ECALL_U, 8
# 页表放在物理地址0x80400000，虚拟地址0
# 0x80000000--0x803fffff恒等映射
# 0x80400000--0x807fffff映射到从0开始
.equ SATP_INIT, 0x80080400
.section .text
.globl _start
_start:
.init:
# 设置内核栈
li sp, KERNEL_STACK_INIT
# 设置用户栈
li t0, USER_STACK_INIT
csrw mscratch, t0
# 设置异常处理地址寄存器 mtvec
la t0, EXCEPTION_HANDLER
csrw mtvec, t0
# 设置页表自映射
li a0, 0x80400000 # PD
li a1, 0x80401000 # PT for PPN1=0
li t0, 0x201004f1 # 0->PT page 0x80401
sw t0, 0(a0)
li t0, 0x201000ff # 0->page 0x80400
sw t0, 0(a1)
li t0, 0x201004ff # 1->page 0x80401
sw t0, 0x4(a1)
# 设置程序段映射
li t0, 0x200000ff # super page begin with page 0x80000
li t1, 0x800
add t1, t1, a0
sw t0, 0(t1)
# 开启页表
li t0, SATP_INIT
csrw satp, t0
la a0, USER
j .OP_G

.OP_G: # 执行用户程序，入口a0
    csrw mepc, a0                  # 用户程序入口写入EPC
    li a0, MSTATUS_MPP_MASK
    csrc mstatus, a0                # 设置 MPP=0，对应 U-mode
    csrrw sp, mscratch, sp # 切用户栈
    li t0, 0x20 # MIP_STIP
    csrs mie, t0
    csrs mip, t0 # 开启suervisor timer interrupt
    li a0, 0x80400000
    lw t0, 0(a0)
    mret

EXCEPTION_HANDLER:
    csrrw sp, mscratch, sp          # 交换 mscratch 和 sp
    csrr a1, mepc
    csrr a2, mtval
    csrr a0, mcause
    # mcause | mepc | mtval
    li t1, EX_ECALL_U
    beq t1, a0, .HANDLE_ECALL
    j .END

.HANDLE_ECALL:
    csrr t0, mepc
    addi t0, t0, 0x4
    csrw mepc, t0

    mv t0, s0
    li t1, SYS_exit
    beq t0, t1, .HANDLE_ECALL_EXIT
    j .END

.HANDLE_ECALL_EXIT:
j .END

.END:
li ra, 0
ret

USER:
rdcycle s3
rdcycleh s4
rdinstret s5
rdinstreth s6
li a0, 0
li a1, 0x1000
lw t0, 0(a0)
li t1, 0x800
add t1, t1, a0
lw t0, 0(t1)
lw t0, 0(a1)
lw t0, 0x4(a1)
li s0, SYS_exit
ecall
