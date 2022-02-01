UTEST_4MDCT:
    lui t0, 0x0100
    addi sp, sp, -4
.LC3:
    sw t0, 0(sp)
    lw t1, 0(sp)
    addi t1, t1, -1
    sw t1, 0(sp)
    lw t0, 0(sp)
    bne t0, zero, .LC3
    addi sp, sp, 4
    jr ra
