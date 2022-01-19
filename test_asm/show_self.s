.section .text
.globl _start
_start:
.OP_D:                          
    addi sp, sp, -8
    # sw s1, 0(sp)
    # sw s2, 4(sp)
    li s1, 0x80000000
    li s2, 48
    li t0, 0x80400000
.LC2:
    lb a0, 0(s1)
    addi s2, s2, -1
    sb a0, 0(t0)     
    addi s1, s1, 0x1      
    addi t0, t0, 0x1
    bne s2, zero, .LC2          

    # lw s1, 0(sp)        
    # lw s2, 4(sp)
    addi sp, sp, 8
    j .DONE

.DONE:
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
