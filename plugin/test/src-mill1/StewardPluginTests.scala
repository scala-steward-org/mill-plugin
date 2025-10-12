package org.scalasteward.mill.plugin

import mill.*
import mill.scalalib.*
import mill.testkit.*
import mill.api.Discover

import org.scalasteward.mill.plugin.StewardPlugin

class StewardPluginTests extends munit.FunSuite {
  test("simple") {
    object build extends TestRootModule {
      lazy val millDiscover = Discover[this.type]

      object minimal extends ScalaModule {
        def scalaVersion = "3.3.4"

        object test extends ScalaTests with TestModule.Munit {
          override def mvnDeps = Seq(mvn"org.scalameta::munit:0.7.29")
        }
      }

      object other extends ScalaModule {
        def scalaVersion = "2.13.8"
      }
    }

    val sourceRoot = os.Path(sys.env("MILL_TEST_RESOURCE_DIR")) / "simple"

    UnitTester(build, sourceRoot).scoped { eval =>
      val Right(UnitTester.Result(result, _)) = eval(StewardPlugin.extractDeps(eval.evaluator)): @unchecked
      val str = result.toString
      assert(str.contains("munit_3"))
      assert(str.contains("scala-library"))
    }
  }
}
