li a0, 1
li a1, 2
andn a2, a0, a1
li a3, 0x80400000
sw a2, 0(a3)
