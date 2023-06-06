package org.scalasteward.mill.plugin

import coursier.core.Repository
import mill.Task
import mill.define.{Discover, ExternalModule}
import mill.scalalib.JavaModule

object StewardPlugin extends ExternalModule with StewardPluginBase {

  lazy val millDiscover: Discover[StewardPlugin.this.type] = Discover[this.type]

  override protected def moduleRepositories(module: JavaModule): Task[Seq[Repository]] = module.repositoriesTask

}
