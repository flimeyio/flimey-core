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

package model.auth

import java.sql.Timestamp

import model.user.Role
import model.user.Role.Role

/**
 * The AuthSession data class.<br />
 * An AuthSession stores session related data while a User is logged in.
 * It is always used together with the Access data class.
 *
 * @param id unique identifier
 * @param session session key
 * @param role role of the User
 * @param status log in status
 * @param userId id of the User
 * @param created creation timestamp (of the underlying entity)
 */
case class AuthSession(id: Long, session: String, role: Role, status: Boolean, userId: Long, created: Timestamp)

object AuthSession {

  def applyRaw (id: Long, session: String, role: String, status: Boolean, userId: Long, created: Timestamp): AuthSession =
    AuthSession(id, session, Role.withName(role), status, userId, created)

  def unapplyToRaw(arg: AuthSession): Option[(Long, String, String, Boolean, Long, Timestamp)] =
    Option((arg.id, arg.session, arg.role.toString, arg.status, arg.userId, arg.created))

  val tupledRaw: ((Long, String, String, Boolean, Long, Timestamp)) => AuthSession = (this.applyRaw _).tupled

}