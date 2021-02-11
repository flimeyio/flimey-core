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

package modules.user.service

import java.util.UUID

import modules.util.messages.{ERR, OK, Status}
import modules.user.model.Role.Role
import modules.user.model.{Role, User}

/**
 * The UserLogic object provides static functionality to process and verify User related data.
 */
object UserLogic extends CredentialProcessor with PasswordProcessor {

  /**
   * Check, if email and password are valid (only literally)
   *
   * @param email email address
   * @param password unencrypted password
   * @return
   */
  def isValidAuthenticationData(email: String, password: String): Status = {
    //email has not to be checked, this is done by the play form way back in the frontend
    val passwordValidation = validatePassword(password)
    if(!passwordValidation.valid) return passwordValidation
    OK()
  }

  /**
   * Map new User credentials to an existing unauthenticated User object.
   * The password is hashed here.
   *
   * @param user unauthenticated User
   * @param email email address
   * @param password unencrypted password
   * @return
   */
  def updateCredentialsOnAuthentication(user: User, email: String, password: String): User = {
    val hashedPassword = hashPassword(password)
    User(user.id, user.username, Option(email), Option(hashedPassword), user.role, None, accepted = true, enabled = true)
  }

  /**
   * Verify the invitation data (literally).
   * Checks if the role string matches a role and the userName matches [[CredentialProcessor.validateUsername]]
   *
   * @param userName username (unique)
   * @param role role string value
   * @return
   */
  def isValidInvitationData(userName: String, role: String): Status = {
    try{
      val roleT: Role = Role.withName(role)
      if(roleT == Role.SYSTEM) throw new Exception("System users can not be created")
    } catch {
      case e: Throwable => return ERR(e.getMessage)
    }
    validateUsername(userName)
  }

  /**
   * Check if a role string matches a Role. Throws an exception otherwise
   *
   * @param role role string value
   * @return Role
   */
  def parseRole(role: String): Role = {
    try{
      Role.withName(role)
    } catch {
      case e: Throwable => throw new Exception("Invalid role")
    }
  }

  /**
   * Map the invitation data to a User object.
   * The returned User can be stored in the repository without further mappings.
   * <br/>
   * The authentication key is generated here.
   *
   * @param userName verified username
   * @param role verified role
   * @return not authenticated User object
   */
  def createInvitedUser(userName: String, role: String): User = {
    val authKey = UUID.randomUUID().toString
    User(0, userName, None, None, Role.withName(role), Option(authKey), accepted = false, enabled = false)
  }
}
