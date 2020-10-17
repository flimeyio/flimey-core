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

package services.auth

import java.util.UUID.randomUUID

import com.google.inject.Inject
import db.auth.SessionRepository
import db.group.GroupMembershipRepository
import db.user.UserRepository
import model.auth.{Access, AuthSession, Ticket}
import model.group.Group

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic for Session and Authentication Management.
 * This class is normally used by dependency injection inside controller endpoints.
 *
 * @param userRepository            injected db interface for User entities.
 * @param sessionRepository         injected db interface for Sessions and Access rights.
 * @param groupMembershipRepository injected db interface for Groups and Memberships.
 */
class AuthService @Inject()(userRepository: UserRepository,
                            sessionRepository: SessionRepository,
                            groupMembershipRepository: GroupMembershipRepository) {

  /**
   * Create a new Session for an existing User.<br />
   * The User has to provide the correct password and email.
   * If those values are verified successfully, a new session is generated with all rights and groups the User has
   * and is stored in the separate session repository.
   * The key to this session object is returned.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param email    the users email address
   * @param password the users password
   * @return key of the generated session object
   */
  def createSession(email: String, password: String): Future[String] = {
    try {
      //FIXME password is not encrypted yet, so check is only based on plaintext
      userRepository.getByEMail(email) flatMap (userRes => {
        if (userRes.isEmpty || userRes.get.password != password) throw new Exception("Wrong E-Mail or Password")
        val user = userRes.get
        groupMembershipRepository.get(user.id) flatMap (groups => {
          val sessionKey = randomUUID().toString
          //access id and session id (also foreign key) can be set to 0, the repository will replace them with actual values
          //the same goes for the timestamp, which is set by sql to NOW
          val session = AuthSession(0, sessionKey, user.role, status = true, user.id, null)
          val accesses = groups.map(group => Access(0, 0, group.id, group.name))
          sessionRepository.add(session, accesses) map (sessionId => {
            //the returned sessionKey (uuid) is combined with the session id to be able to extract both on request from
            //the client. Because the session token is encrypted anyways, this introduces no security issues.
            AuthLogic.createCompoundKey(sessionKey, sessionId)
          })
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Builds the authentication Ticket associated to a sessionCompoundKey (the users session key).<br />
   * This operation is successful, if the user has been previously logged in and an AuthSession entry for the given
   * key exists in the auth database.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param sessionCompoundKey the session key of the user request
   * @return ticket object containing all access rights for further authentication
   */
  def getTicket(sessionCompoundKey: String): Future[Ticket] = {
    try {
      val decomposedCompoundKey = AuthLogic.resolveCompoundKey(sessionCompoundKey)
      val sessionKey = decomposedCompoundKey._1
      val sessionId = decomposedCompoundKey._2
      sessionRepository.getComplete(sessionId) map (authInfo => {
        if (authInfo.isEmpty) throw new Exception("No valid Login")
        val authSession = authInfo.get._1
        val groups = authInfo.get._2.map(access => Group(access.groupId, access.groupName))
        if (authSession.session != sessionKey) throw new Exception("Invalid Session")
        Ticket(authSession, groups)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete an AuthSession entry in the auth database.<br />
   * Afterwards the User will not be able to access protected content and must renew his log in.
   * However, only the session of the requesting device is removed. All other sessions (other devices) remain logged in.
   * <br />
   * After this call, the calling controller should notify the client and unset his request session. Otherwise, the following
   * request will fail (wanted behaviour).
   * <br />
   * This is a safe implementation and can be used by controller classes.
   * @param ticket
   * @return
   */
  def deleteSession()(implicit ticket: Ticket): Future[Unit] = {
    try {
      sessionRepository.delete(ticket.authSession.id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

}
