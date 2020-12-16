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

package group.model

/**
 * Model and db entity class representing the relation between two groups.
 *
 * @param id unique identifier
 * @param targetId id of the target group
 * @param viewerId id of the viewer group (which can do something to the target)
 * @param role role of the viewer group regarding the target
 */
case class Viewer (id: Long, targetId: Long, viewerId: Long, role: ViewerRole.Role)

object Viewer {

  def applyRaw (id: Long, targetId: Long, viewerId: Long, role: String): Viewer = {
    Viewer(id, targetId, viewerId, ViewerRole.withName(role))
  }

  def unapplyToRaw(arg: Viewer): Option[(Long, Long, Long, String)] =
    Option((arg.id, arg.targetId, arg.viewerId, arg.role.toString))

  val tupledRaw: ((Long, Long, Long, String)) => Viewer = (this.applyRaw _).tupled

}
