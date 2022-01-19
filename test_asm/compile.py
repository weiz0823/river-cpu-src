import sys
import os

if len(sys.argv) != 2:
    print("Usage: python compile.py {assembly}")
    exit()
else:
    filename = sys.argv[1]
    filename_without_ext = os.path.splitext(filename)[0]
    os.system(f"riscv64-unknown-elf-gcc -march=rv32i_zbb -mabi=ilp32 -nostdlib -nostdinc -static -Ttext 0x80000000 {filename} -o {filename_without_ext}.elf ")
    os.system(f"riscv64-unknown-elf-objcopy -O binary {filename_without_ext}.elf {filename_without_ext}.bin")