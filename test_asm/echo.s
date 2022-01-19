.section .text
.globl _start
_start:
nop
SHELL:
jal READ_SERIAL
jal WRITE_SERIAL
j .DONE
READ_SERIAL:                        
    li t0, 0x10000000
    li t2, 0x80400000
.TESTR:
    lb t1, 5(t0)
    andi t1, t1, 1         
    bne t1, zero, .RSERIAL          
    j .TESTR                        
.RSERIAL:
    lb a0, 0(t0)
    sb a0, 0(t2)
    jr ra
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
.DONE:
    j SHELL
