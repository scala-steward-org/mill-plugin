package mill.scalasteward.internal

import mill.api.Evaluator
import mill.api.internal.RootModule0

/**
 * Expose private[mill] def rootModule: RootModule0
 * in mill.api.Evaluator
 */
def getRootModule(ev: Evaluator): RootModule0 = ev.rootModule
