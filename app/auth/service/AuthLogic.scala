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

package auth.service

import java.util.UUID.randomUUID

import auth.model.{Access, AuthSession}
import user.model.{Group, User}
import user.service.PasswordProcessor

/**
 * The AuthLogic object provides static functionality to process, verify and validate
 * sessions and authentication related data.
 */
object AuthLogic extends SessionProcessor with PasswordProcessor {

  /**
   * Generate Session data from User data.<br />
   * Creates a random session key.
   *
   * @param user to create the session for
   * @param groups access rights of the User
   * @return session data
   */
  def createSession(user: User, groups: Seq[Group]): (AuthSession, Seq[Access]) = {
    //create random key (uuid is random enough)
    val sessionKey = randomUUID().toString
    //access id and session id (also foreign key) can be set to 0, the repository will replace them with actual values
    //the same goes for the timestamp, which is set by sql to NOW
    val session = AuthSession(0, sessionKey, user.role, status = true, user.id, null)
    val accesses = groups.map(group => Access(0, 0, group.id, group.name))
    (session, accesses)
  }

  /**
   * Map Access data to Group data.
   *
   * @param accesses Accesses of a User
   * @return Groups the User is member of
   */
  def generateGroupsFromAccessRights(accesses: Seq[Access]): Seq[Group] = {
    accesses.map(access => Group(access.groupId, access.groupName))
  }

}
