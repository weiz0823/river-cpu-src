package config

case class DeviceConfig(
    baseRam: SramConfig = SramConfig(usePrefetch = false),
    extRam: SramConfig = SramConfig(usePrefetch = false),
    uart: UartConfig = UartConfig()
) {}

case class SramConfig(
    val usePrefetch: Boolean
) {
  val rdCycles: Int = 2;
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
