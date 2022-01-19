package config

case class MemoryMapConfig(
    baseRam: AddressConfig = AddressConfig(0x80400000L, 22),
    extRam: AddressConfig = AddressConfig(0x80000000L, 22),
    uart: AddressConfig = AddressConfig(0x10000000, 3),
    clint: AddressConfig = AddressConfig(0x2000000, 16)
) {}
