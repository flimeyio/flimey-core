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

import auth.model.Ticket
import auth.repository.SessionRepository
import com.google.inject.Inject
import group.repository.GroupMembershipRepository
import user.repository.UserRepository

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
      userRepository.getByEMail(email) flatMap (userRes => {
        if (userRes.isEmpty) throw new Exception("Wrong E-Mail")
        val user = userRes.get
        if(!AuthLogic.checkPassword(user.password.get, password)) throw new Exception("Wrong Password")
        groupMembershipRepository.get(user.id) flatMap (groups => {
          val (session, accesses) = AuthLogic.createSession(user, groups)
          sessionRepository.add(session, accesses) map (sessionId => {
            //the returned sessionKey (uuid) is combined with the session id to be able to extract both on request from
            //the client. Because the session token is encrypted anyways, this introduces no security issues.
            AuthLogic.createCompoundKey(session.session, sessionId)
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
      val (sessionKey, sessionId) = AuthLogic.resolveCompoundKey(sessionCompoundKey)
      sessionRepository.getComplete(sessionId) map (authInfo => {
        if (authInfo.isEmpty) throw new Exception("No valid Login")
        val (authSession, accesses) = authInfo.get
        if (authSession.session != sessionKey) throw new Exception("Invalid Session")
        val groups = AuthLogic.generateGroupsFromAccessRights(accesses)
        Ticket(authSession, groups)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete an AuthSession entry in the auth database.<br />
   * Afterwards the User will not be able to access protected content and must renew his log in.
   * By default, only the session of the requesting device is removed. All other sessions (other devices) remain logged in.
   * However if the global flag is set, all sessions of this user will be removed.
   * <br />
   * After this call, the calling controller should notify the client and unset his request session. Otherwise, the following
   * request will fail (wanted behaviour).
   * <br />
   * This is a safe implementation and can be used by controller classes.
   * <br />
   * Fails without WORKER rights.
   *
   * @param ticket implicit authentication data
   * @return
   */
  def deleteSession(all: Option[Boolean])(implicit ticket: Ticket): Future[Unit] = {
    try {
      if(all.isDefined && all.get){
        sessionRepository.deleteAll(ticket.authSession.userId)
      }else {
        sessionRepository.delete(ticket.authSession.id)
      }
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

}
