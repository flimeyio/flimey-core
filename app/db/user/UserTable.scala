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

package db.user

import model.user.User
import slick.jdbc.MySQLProfile.api._

/**
 * Slick framework db mapping for User.
 * See evolutions/default for schema creation
 *
 * @param tag fro mysql
 */
class UserTable(tag: Tag) extends Table[User](tag, "user") {

  def id = column[Long]("id", O.PrimaryKey,O.AutoInc)
  def username = column[String]("username")
  def email = column[String]("email", O.Unique)
  def password = column[String]("password")
  def role = column[String]("role")
  def key = column[String]("auth_key")
  def accepted = column[Boolean]("accepted")
  def enabled = column[Boolean]("enabled")

  override def * = (id, username, email, password, role, key, accepted,enabled) <> (User.tupled, User.unapply)

}
