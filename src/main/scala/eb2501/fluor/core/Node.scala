package eb2501.fluor
package core

trait Node:
  def isCached: Boolean
  def callees: Option[List[Node]]
  def callers: Option[List[Node]]
