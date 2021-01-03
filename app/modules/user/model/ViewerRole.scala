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

/**
 * Enumeration to represent the rights a Group has in a viewer relation to another Group, Asset or Collection.
 */
object ViewerRole extends Enumeration {

  type Role = Value

  val VIEWER, EDITOR, MAINTAINER = Value

  def isAtLeastViewer(role: Role): Boolean = {
    role == VIEWER || role == EDITOR || role == MAINTAINER
  }

  def isAtLeastEditor(role: Role): Boolean = {
    role == EDITOR || role == MAINTAINER
  }

  def isAtLeastMaintainer(role: Role): Boolean = {
    role == MAINTAINER
  }

  def parseViewerRole(viewerRole: String): ViewerRole.Role = {
    try{
      ViewerRole.withName(viewerRole)
    }catch {
      case e: Throwable => throw new Exception("Invalid viewer role")
    }
  }

}
