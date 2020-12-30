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

package auth.model

import user.model.ViewerRole

/**
 * The Access data class.<br />
 * Represents a runtime oriented mapping of Group based rights associated to an active AuthSession.
 *
 * @param id unique identifier
 * @param sessionId id of the associated AuthSession
 * @param groupId id of the associated Group
 * @param groupName name of the associated Group
 */
case class Access(id: Long, sessionId: Long, groupId: Long, groupName: String, role: ViewerRole.Role)

object Access {

  def applyRaw (id: Long, sessionId: Long, groupId: Long, groupName: String, role: String): Access =
    Access(id, sessionId, groupId, groupName, ViewerRole.withName(role))

  def unapplyToRaw(arg: Access): Option[(Long, Long, Long, String, String)] =
    Option((arg.id, arg.sessionId, arg.groupId, arg.groupName, arg.role.toString))

  val tupledRaw: ((Long, Long, Long, String, String)) => Access = (this.applyRaw _).tupled

}
