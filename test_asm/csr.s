.section .text
.globl _start
_start:
mv t0, x0
mv t1, x0
li t2, 0x10
li a0, 0x80400000
csrw mscratch, zero
csrw sscratch, zero
li t0, 0x5a5a5a5a
li t1, 0x3
csrw mscratch, t0
csrrs t2, mscratch, t1
csrrw t0, sscratch, t0
csrc sscratch, t1
sw t0, 0(a0)
sw t1, 4(a0)
sw t2, 8(a0)
