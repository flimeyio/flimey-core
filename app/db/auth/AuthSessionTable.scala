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

package db.auth

import java.sql.Timestamp

import model.auth.AuthSession
import slick.jdbc.MySQLProfile.api._

class AuthSessionTable(tag: Tag) extends Table[AuthSession](tag, "session") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def session = column[String]("session")
  def role = column[String]("role")
  def status = column[Boolean]("status")
  def userId = column[Long]("user_id")
  def created = column[Timestamp]("created", O.SqlType("datetime not null default CURRENT_TIMESTAMP"))

  override def * = (id, session, role, status, userId, created) <> (AuthSession.tupled, AuthSession.unapply)

}
