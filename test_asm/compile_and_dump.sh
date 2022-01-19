#!/usr/bin/env bash
base_name=${1%\.s}
elf_name=$base_name.elf
bin_name=$base_name.bin
riscv64-unknown-elf-c++ \
	-nostdlib -nostdinc -static -g \
	-Ttext 0x80000000 \
	-march=rv32i -mabi=ilp32 \
	-o $elf_name $1
riscv64-unknown-elf-objdump -d $elf_name
riscv64-unknown-elf-objcopy \
    -j .text -j .text.* \
	-j .rodata -j .data \
	-O binary -v \
	$elf_name $bin_name
