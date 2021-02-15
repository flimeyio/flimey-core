/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020  Karl Kegel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * */

package modules.user.model

object Role extends Enumeration {

  type Role = Value

  val WORKER, MODELER, ADMIN, SYSTEM = Value

  def isAtLeastAdmin(role: Role): Boolean = {
    role == ADMIN || role == SYSTEM
  }

  def isAtLeastModeler(role: Role): Boolean = {
    role == MODELER || role == SYSTEM
  }

  def isAtLeastWorker(role: Role): Boolean = {
    role == MODELER || role == SYSTEM || role == ADMIN || role == WORKER
  }

  def isAtLeastSystem(role: Role): Boolean = {
    role == SYSTEM
  }

  def getAll: Seq[Role.Value] = Seq(WORKER, MODELER, ADMIN, SYSTEM)

}
