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

package auth.repository

import auth.model.Access
import slick.jdbc.MySQLProfile.api._

class AccessTable (tag: Tag) extends Table[Access](tag, "access") {

  def id = column[Long]("id", O.PrimaryKey,O.AutoInc)
  def sessionId = column[Long]("session_id")
  def groupId = column[Long]("group_id")
  def groupName = column[String]("group_name")

  override def * = (id, sessionId, groupId, groupName) <> (Access.tupled, Access.unapply)

}
