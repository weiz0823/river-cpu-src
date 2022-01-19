// build.sc

import mill._, scalalib._

trait CommonSpinalModule extends ScalaModule {
  override def scalaVersion = "2.12.15"

  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit"
  )

  override def ivyDeps = Agg(
    ivy"com.github.spinalhdl::spinalhdl-core:1.6.0",
    ivy"com.github.spinalhdl::spinalhdl-lib:1.6.0"
  )

  override def scalacPluginIvyDeps = Agg(
    ivy"com.github.spinalhdl::spinalhdl-idsl-plugin:1.6.0",
    ivy"org.scalamacros:::paradise:2.1.1"
  )
}

object River extends CommonSpinalModule {
  override def mainClass = Some("top.TopVerilog")
}