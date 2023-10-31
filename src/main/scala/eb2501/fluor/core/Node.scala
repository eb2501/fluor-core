package eb2501.fluor.core

trait Node:
  def isCached: Boolean
  def callees: Option[List[Node]]
  def callers: Option[List[Node]]
