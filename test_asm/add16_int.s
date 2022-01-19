.equ KERNEL_STACK_INIT, 0x80800000
.equ USER_STACK_INIT, 0x807F0000
.equ MSTATUS_MPP_MASK, 0x1800
.equ SYS_exit, 1
.equ EX_ECALL_U, 8
.equ SYS_putc, 30
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
    csrr t0, mcause
    li t1, EX_ECALL_U
    beq t1, t0, .HANDLE_ECALL
    j .END

.HANDLE_ECALL:
    csrr t0, mepc
    addi t0, t0, 0x4
    csrw mepc, t0

    mv t0, s0
    li t1, SYS_exit
    beq t0, t1, .HANDLE_ECALL_EXIT
    li t1, SYS_putc
    beq t0, t1, .HANDLE_ECALL_PUTC
    j .END

.HANDLE_ECALL_EXIT:
li a0, 0x45
jal WRITE_SERIAL
j .END

.HANDLE_ECALL_PUTC:
jal WRITE_SERIAL
j CONTEXT_SWITCH

CONTEXT_SWITCH:
mret

.END: # 死循环
beq zero, zero, .END

WRITE_SERIAL:                     
    li t0, 0x10000000
.TESTW:
    lb t1, 5(t0) 
    andi t1, t1, 0x20 
    bne t1, zero, .WSERIAL        
    j .TESTW                    
.WSERIAL:
    sb a0, 0(t0)
    jr ra

USER:
mv t0, x0
mv t1, x0
li t2, 0x10
li a0, 0x80400000
loop:
addi t0, t0, 1
add t1, t1, t0
bne t0, t2, loop
sw t1, 0(a0)
li a0, 0x64
li s0, SYS_putc
ecall
li a0, 0x6f
li s0, SYS_putc
ecall
li a0, 0x6e
li s0, SYS_putc
ecall
li a0, 0x65
li s0, SYS_putc
ecall
li a0, 0x21
li s0, SYS_putc
ecall
li s0, SYS_exit
ecall
