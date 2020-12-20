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

package user.service

import user.model.ViewerRole

trait ViewerProcessor {

  /**
   * Transform a ViewerRole string in its enum representation.
   * Throws an exception, if an invalid string is passed.
   *
   * @param viewerRole string value of a ViewerRole
   * @return ViewerRole
   */
  def parseViewerRole(viewerRole: String): ViewerRole.Role = {
    try{
      ViewerRole.withName(viewerRole)
    }catch {
      case e: Throwable => throw new Exception("Invalid viewer role")
    }
  }

}
