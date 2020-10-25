package model.user

object Role extends Enumeration {

  type Role = Value

  val WORKER, MODELER, ADMIN, SYSTEM = Value

  def isAtLeastAdmin(role: Role): Boolean = {
    role == ADMIN || role == SYSTEM
  }

  def isAtLeastModeler(role: Role): Boolean = {
    role == MODELER || role == SYSTEM
  }

}
