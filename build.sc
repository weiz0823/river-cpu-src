// build.sc
import mill._, scalalib._, publish._
//import $ivy.`com.lihaoyi::mill-contrib-bloop:0.9.9`

trait CommonSpinalModule extends ScalaModule {
  override def scalaVersion = "2.12.15"
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit"
  )

  def ivyDeps = Agg(
    ivy"com.github.spinalhdl::spinalhdl-core:1.6.0",
    ivy"com.github.spinalhdl::spinalhdl-lib:1.6.0"
  )
  def scalacPluginIvyDeps = Agg(
    ivy"com.github.spinalhdl::spinalhdl-idsl-plugin:1.6.0",
    ivy"org.scalamacros:::paradise:2.1.1"
  )
}

object River extends CommonSpinalModule {
  def mainClass = Some("top.TopVerilog")
}

object RiverOS extends CommonSpinalModule {
  def moduleDeps = Seq(River)

  def mainClass = Some("RiverOS")
}
