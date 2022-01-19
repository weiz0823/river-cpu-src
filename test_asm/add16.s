.section .text
.globl _start
_start:
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
jal WRITE_SERIAL
li a0, 0x6f
jal WRITE_SERIAL
li a0, 0x6e
jal WRITE_SERIAL
li a0, 0x65
jal WRITE_SERIAL
li a0, 0x21
jal WRITE_SERIAL
end:
beq zero, zero, end
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
