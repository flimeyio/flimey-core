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

import slick.jdbc.MySQLProfile.api._
import modules.user.model.Group

/**
 * Slick framework db mapping for Groups
 * see evolutions/default for schema creation.
 *
 * @param tag for mysql
 */
class GroupTable(tag: Tag) extends Table[Group](tag, "u_group") {

  def id = column[Long]("id",  O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.Unique)

  override def * = (id, name) <> (Group.tupled, Group.unapply)

}
