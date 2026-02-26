// plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.3`
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.1`

import mill._
import mill.define.DynamicModule
import mill.scalalib._
import mill.scalalib.api.JvmWorkerUtil
import mill.scalalib.scalafmt.ScalafmtModule
import mill.scalalib.publish.{PomSettings, License, VersionControl, Developer}
import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version.VcsVersion

trait PlatformConfig {
  def millVersion: String
  def millPlatform: String
  def scalaVersion: String = "3.7.4"
  def testWith: Seq[String]

  def millLibs = mvn"com.lihaoyi::mill-libs:${millVersion}"
}
object Mill1 extends PlatformConfig {
  override val millVersion = "1.0.0" // scala-steward:off
  override val millPlatform = "1"
  override val testWith = Seq("1.0.5", millVersion)
}
object Mill011 extends PlatformConfig {
  override val millVersion = "0.11.0" // scala-steward:off
  override val millPlatform = "0.11"
  override val scalaVersion = "2.13.18"
  override val millLibs = mvn"com.lihaoyi::mill-scalalib:${millVersion}"
  override val testWith = Seq("0.12.14", "0.12.0", "0.11.13", millVersion)
}
object Mill010 extends PlatformConfig {
  override val millVersion = "0.10.0" // scala-steward:off
  override val millPlatform = "0.10"
  override val scalaVersion = "2.13.18"
  override val millLibs = mvn"com.lihaoyi::mill-scalalib:${millVersion}"
  override val testWith = Seq("0.10.15", millVersion)
}
object Mill09 extends PlatformConfig {
  override val millVersion = "0.9.3" // scala-steward:off
  override val millPlatform = "0.9"
  override val scalaVersion = "2.13.18"
  override val millLibs = mvn"com.lihaoyi::mill-scalalib:${millVersion}"
  override val testWith = Seq("0.9.12", millVersion)
}
object Mill07 extends PlatformConfig {
  override val millVersion = "0.7.0" // scala-steward:off
  override val millPlatform = "0.7"
  override val scalaVersion = "2.13.18"
  override val millLibs = mvn"com.lihaoyi::mill-scalalib:${millVersion}"
  override val testWith = Seq("0.8.0", "0.7.4", millVersion)
}
object Mill06 extends PlatformConfig {
  override val millVersion = "0.6.0" // scala-steward:off
  override val millPlatform = "0.6"
  override val scalaVersion = "2.12.21"
  override val millLibs = mvn"com.lihaoyi::mill-scalalib:${millVersion}"
  override val testWith = Seq("0.6.3", millVersion)
}

val platforms: Seq[PlatformConfig] = Seq(Mill1, Mill011, Mill010, Mill09, Mill07, Mill06)
val testVersions = platforms.tail.flatMap(p => p.testWith)

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

object plugin extends Cross[PluginCross](platforms.map(_.millPlatform))
trait PluginCross
    extends ScalaModule
    with PublishConfig
    with ScalafmtModule
    with DynamicModule
    with Cross.Module[String] {
  val millPlatform = crossValue
  val config: PlatformConfig = platforms.find(_.millPlatform == millPlatform).head
  override def scalaVersion: T[String] = config.scalaVersion
  override def artifactName: T[String] = "scala-steward-mill-plugin"
  override def platformSuffix: T[String] = s"_mill${millPlatform}"
  override def scalacOptions = Seq(
    "-release",
    "8",
    "-encoding",
    "UTF-8",
    "-Ywarn-unused",
    "-deprecation"
  )
  override def compileIvyDeps: T[Agg[Dep]] = super.compileIvyDeps() ++ Agg(
    config.millLibs
  )
//  override def sources: Sources = T.sources {
//    super.sources() ++ Seq(PathRef(millSourcePath / s"src-mill${config.millPlatform}"))
//  }
  override def sources = Task.Sources {
    Seq(PathRef(moduleDir / "src")) ++
      (JvmWorkerUtil.matchingVersions(millPlatform) ++
        JvmWorkerUtil.versionRanges(millPlatform, platforms.map(_.millPlatform)))
        .map(p => PathRef(moduleDir / s"src-mill${p}"))
  }

  def millModuleDirectChildren = super.millModuleDirectChildren
    .filterNot { m =>
      val major = config.millPlatform.split('.').head.toInt
      // Ignore test module for mill older than 1
      major < 1 && m == test
    }
  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = super.ivyDeps() ++ Seq(
      mvn"com.lihaoyi::mill-testkit:${config.millVersion}",
      config.millLibs,
      mvn"org.scalameta::munit:1.2.3"
    )
  }
}

object itest extends Cross[ItestCross](testVersions)
trait ItestCross extends MillIntegrationTestModule with Cross.Module[String] {
  val testVersion = crossValue
  val config: PlatformConfig = platforms.find(_.testWith.contains(testVersion)).head
  override def millTestVersion: T[String] = testVersion
  override def pluginsUnderTest = Seq(plugin(config.millPlatform))
  def sources = Task.Sources {
    super.sources() ++
      (JvmWorkerUtil.matchingVersions(config.millPlatform) ++
        JvmWorkerUtil.versionRanges(config.millPlatform, platforms.map(_.millPlatform)))
        .map(p => PathRef(moduleDir / s"src-mill${p}"))
  }
  val testBase = moduleDir / "src"
  override def testInvocations: T[Seq[(PathRef, Seq[TestInvocation.Targets])]] = Task {
    testCases().map { pathRef =>
      pathRef -> Seq(
        TestInvocation.Targets(Seq("verify"), noServer = true)
      )
    }
  }
}
