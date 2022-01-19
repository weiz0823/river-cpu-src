.equ KERNEL_STACK_INIT, 0x80800000
.equ USER_STACK_INIT, 0x807F0000
.equ MSTATUS_MPP_MASK, 0x1800
.equ SYS_exit, 1
.equ EX_ECALL_U, 8
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
la s0, EXCEPTION_HANDLER
csrw mtvec, s0
la a0, USER
j .OP_G

.OP_G: # 执行用户程序，入口a0
    csrw mepc, a0                  # 用户程序入口写入EPC
    li a0, MSTATUS_MPP_MASK
    csrc mstatus, a0                # 设置 MPP=0，对应 U-mode
    csrrw sp, mscratch, sp # 切用户栈
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
    nop
    nop
    j .END
    nop
    nop

.HANDLE_ECALL_EXIT:
j .END
nop
nop

.END:
li t0, 0
jr 0(t0)

USER:
rdcycle s3
rdcycleh s4
rdinstret s5
rdinstreth s6
li s0, SYS_exit
ecall
