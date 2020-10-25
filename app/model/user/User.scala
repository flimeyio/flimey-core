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

package model.user

import model.user.Role.Role

/**
 * The User data class.
 *
 * @param id unique identifier
 * @param username visible name of the user
 * @param email unique email address
 * @param password password (hashed in db)
 * @param role access role
 * @param key pending authentication key
 * @param accepted flag if user has accepted the usage conditions
 * @param enabled flag if the user is allowed to log in
 */
case class User (id: Long, username: String, email: String, password: String, role: Role,
                 key: String, accepted: Boolean, enabled: Boolean)

object User {

  def applyRaw (id: Long, username: String, email: String, password: String, role: String,
                key: String, accepted: Boolean, enabled: Boolean): User =
    User(id, username, email, password, Role.withName(role), key, accepted, enabled)

  def unapplyToRaw(arg: User): Option[(Long, String, String, String, String, String, Boolean, Boolean)] =
    Option((arg.id, arg.username, arg.email, arg.password, arg.role.toString, arg.key, arg.accepted, arg.enabled))

  val tupledRaw: ((Long, String, String, String, String, String, Boolean, Boolean)) => User = (this.applyRaw _).tupled

}
