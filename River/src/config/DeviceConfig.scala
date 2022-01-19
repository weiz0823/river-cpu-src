package config

case class DeviceConfig(
    sram: SramConfig = SramConfig(),
    uart: UartConfig = UartConfig()
) {}

case class SramConfig(
    // ignored, 1 cycle read is impossible
    rdCycles: Int = 2
) {
  // value ignored
  val wrCycles: Int = 2;
}

case class UartConfig(
    // ignored, 1 cycle read is impossible
    rdCycles: Int = 2
) {
  // ignored
  val wrCycles = 2;
  // ignored
  val uart16550E = false;
}
