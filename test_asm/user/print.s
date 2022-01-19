.section .text
.globl _start
_start:
addi sp, sp, -12
sw ra, 8(sp)
sw s1, 4(sp)
sw s2, 0(sp)
ori s1, x0, 0x21
ori s2, x0, 0x7F
loop:
mv a0, s1
li s0, 30
ecall
addi s1, s1, 1
bne s1, s2, loop
lw ra, 8(sp)
lw s1, 4(sp)
lw s2, 0(sp)
addi sp, sp, 12
ret
