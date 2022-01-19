"""Dump program in hexidecimal string, for direct copy into testbench."""
import sys

if len(sys.argv) != 2:
    print(f"Usage: {sys.argv[0]} your_program.bin")
    exit(1)

ls = []
with open(sys.argv[1], "rb") as fd:
    while True:
        s = fd.read(4)
        if len(s) == 0:
            break
        v = 0
        for i in range(3, -1, -1):
            v <<= 8
            v |= s[i]
        ls.append(hex(v) + "L")
print(*ls, sep=",")
