/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020-2021 Karl Kegel
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

package modules.core.model

/**
 * The Viewer data class to model the possible relationships between two objects within the system.
 * <p> 1. the target is a subclass of [[FlimeyEntity]] and the viewer is a Group - this specifies the access rights of
 * the Group members to the entity (if the can ready, edit, ...)
 * <p> 2. the target is Group and the viewer is a Group - this specifies a transition of rights. So the viewer Group is
 * granted rights of entities the target Group possess.
 * <p> Has a repository representation.
 *
 * @param id unique identifier
 * @param targetId id of the target [[FlimeyEntity]] or Group
 * @param viewerId id of the viewer Group (which can do something to the target)
 * @param role role of the viewer Group regarding the target - see [[ViewerRole]]
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
