// plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.0`
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.3.0`

// imports
import mill._
import mill.define.Sources
import mill.scalalib._
import mill.scalalib.scalafmt.ScalafmtModule
import mill.scalalib.publish.{PomSettings, License, VersionControl, Developer}
import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version.VcsVersion

trait PlatformConfig {
  def millVersion: String
  def millPlatform: String
  def scalaVersion: String
  def testWith: Seq[String]

  def millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
}
object Mill010 extends PlatformConfig {
  override val millVersion = "0.10.0" // scala-steward:off
  override val millPlatform = "0.10"
  override val scalaVersion = "2.13.10"
  override val testWith = Seq("0.10.10", millVersion)
}
object Mill09 extends PlatformConfig {
  override val millVersion = "0.9.3" // scala-steward:off
  override val millPlatform = "0.9"
  override val scalaVersion = "2.13.10"
  override val testWith = Seq("0.9.12", millVersion)
}
object Mill07 extends PlatformConfig {
  override val millVersion = "0.7.0" // scala-steward:off
  override val millPlatform = "0.7"
  override val scalaVersion = "2.13.10"
  override val testWith = Seq("0.8.0", "0.7.4", millVersion)
}
object Mill06 extends PlatformConfig {
  override val millVersion = "0.6.0" // scala-steward:off
  override val millPlatform = "0.6"
  override val scalaVersion = "2.12.17"
  override val testWith = Seq("0.6.3", millVersion)
}

val platforms: Seq[PlatformConfig] = Seq(Mill010, Mill09, Mill07, Mill06)
val testVersions = platforms.flatMap(p => p.testWith)

trait PublishConfig extends PublishModule {
  override def publishVersion: T[String] = VcsVersion.vcsState().format()
  override def pomSettings: T[PomSettings] = PomSettings(
    description = "Mill plugin to generate dependency report to be process by scala-steward",
    organization = "org.scala-steward",
    url = "https://github.com/scala-steward-org/mill-plugin",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl
      .github(owner = "scala-steward-org", repo = "mill-plugin"),
    developers =
      Seq(Developer("lefou", "Tobias Roeser", "https://github.com/lefou"))
  )
}

object plugin extends Cross[PluginCross](platforms.map(_.millPlatform): _*)
class PluginCross(millPlatform: String)
    extends ScalaModule
    with PublishConfig
    with ScalafmtModule {

  val config: PlatformConfig = platforms.find(_.millPlatform == millPlatform).head

  override def millSourcePath: os.Path = super.millSourcePath / os.up
  override def scalaVersion: T[String] = config.scalaVersion
  override def artifactName: T[String] = "scala-steward-mill-plugin"
  override def platformSuffix: T[String] = s"_mill${millPlatform}"
  override def artifactId: T[String] =
    artifactName() + platformSuffix() + artifactSuffix()
  override def scalacOptions = Seq("-Ywarn-unused", "-deprecation")
  override def compileIvyDeps: T[Agg[Dep]] = super.compileIvyDeps() ++ Agg(
    config.millScalalib
  )
  override def sources: Sources = T.sources {
    super.sources() ++ Seq(PathRef(millSourcePath / s"src-mill${config.millPlatform}"))
  }
}

object itest extends Cross[ItestCross](testVersions: _*)
class ItestCross(testVersion: String) extends MillIntegrationTestModule {

  override def millSourcePath: os.Path = super.millSourcePath / os.up

  val config: PlatformConfig = platforms.find(_.testWith.contains(testVersion)).head

  override def millTestVersion: T[String] = testVersion

  override def pluginsUnderTest = Seq(plugin(config.millPlatform))

  val testBase = millSourcePath / "src"

  override def testInvocations: T[Seq[(PathRef, Seq[TestInvocation.Targets])]] = T {
    Seq(
      PathRef(testBase / "minimal") -> Seq(
        TestInvocation.Targets(Seq("verify"), noServer = true)
      )
    )
  }
}
