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

package modules.user.repository

import slick.jdbc.PostgresProfile.api._
import modules.user.model.GroupMembership

/**
 * Slick framework db mapping for GroupMembership.
 * see evolutions/default for schema creation.
 *
 * @param tag for mysql
 */
class GroupMembershipTable(tag: Tag) extends Table[GroupMembership](tag, "group_membership")  {

  def id = column[Long]("id", O.PrimaryKey,O.AutoInc)
  def groupId = column[Long]("group_id")
  def userId = column[Long]("user_id")

  override def * = (id, groupId, userId) <> (GroupMembership.tupled, GroupMembership.unapply)

}
