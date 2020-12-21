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

package user.service

import auth.model.Ticket
import com.google.inject.Inject
import user.model.{GroupMembership, GroupStats, Role, User}
import user.repository.{GroupMembershipRepository, UserRepository}
import util.assertions.RoleAssertion

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic for User and Account Management.
 * This class is normally used by dependency injection inside controller endpoints.
 *
 * @param userRepository injected db interface for User entities.
 */
class UserService @Inject()(userRepository: UserRepository, groupMembershipRepository: GroupMembershipRepository) extends RoleAssertion {

  /**
   * Create a new User (invitation).<br />
   * The User is created with an unique username and a role.
   * After such a new User is created, he can not log in until the account is authenticated in a separate step.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param userName unique visible name of the User
   * @param role     represents rights see [[user.model.Role]] management doc for more information
   * @param ticket   implicit authentication ticket
   * @return id of the newly created User
   */
  def createUser(userName: String, role: String)(implicit ticket: Ticket): Future[Long] = {
    try {
      //only admin users can create new accounts/send invitations
      assertAdmin
      val dataStatus = UserLogic.isValidInvitationData(userName, role)
      if (!dataStatus.valid) dataStatus.throwError
      val user = UserLogic.createInvitedUser(userName, role)
      userRepository.add(user)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete an User.<br />
   * This operation works for authenticated AND unauthenticated Users.
   * The User is removed from all Groups.
   * <p> The default SYSTEM User can not be deleted.
   * <p> A User can only be deleted by himself or an admin User.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param userId of the User to delete
   * @param ticket implicit authentication ticket
   * @return
   */
  def deleteUser(userId: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      //only admin users or the User itself can delete an account.
      if (ticket.authSession.userId != userId) {
        assertAdmin
      }
      if (userId == 1) throw new Exception("The default SYSTEM user can not be deleted")
      userRepository.delete(userId)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Authenticate a invited User.<br />
   * Fills the missing fields of a previously invited User and enables the login.
   * Adds the User to the public group.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   * <br />
   * Fails without ADMIN rights.
   *
   * @param key      authentication key (generated on createUser)
   * @param email    unique email
   * @param password login password
   * @param agree    agreement to terms and conditions
   * @return
   */
  def authenticateUser(key: String, email: String, password: String, agree: Boolean): Future[Long] = {
    try {
      if (!agree) throw new Exception("You must agree to the Terms & Conditions to create an account!")
      userRepository.getByKey(key) flatMap (userOption => {
        if (userOption.isEmpty) throw new Exception("Invalid key!")
        val credentialStatus = UserLogic.isValidAuthenticationData(email, password)
        if (!credentialStatus.valid) credentialStatus.throwError
        val userUpdate = UserLogic.updateCredentialsOnAuthentication(userOption.get, email, password)
        userRepository.update(userUpdate) flatMap (_ => groupMembershipRepository.add(GroupMembership(0, GroupStats.PUBLIC_ID, userUpdate.id)))
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all Users which are not yet authenticated.<br />
   * This is a safe implementation and can be used by controller classes.
   * <br />
   * Fails without ADMIN rights.
   *
   * @param ticket implicit authentication ticket
   * @return
   */
  def getAllInvitedUsers()(implicit ticket: Ticket): Future[Seq[User]] = {
    try {
      assertAdmin
      userRepository.getAllWithPendingAuthentication
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all Users which are authenticated.<br />
   * This is a safe implementation and can be used by controller classes.
   * <br />
   * Fails without ADMIN rights.
   *
   * @param ticket implicit authentication ticket
   * @return Future Seq[User]
   */
  def getAllAuthenticatedUsers()(implicit ticket: Ticket): Future[Seq[User]] = {
    try {
      assertAdmin
      userRepository.getAllAuthenticated
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get an authenticated User by id.<br />
   * This is a safe implementation and can be used by controller classes.
   * <br />
   * Fails without ADMIN rights.
   *
   * @param ticket implicit authentication ticket
   * @return Future[User]
   */
  def getAuthenticatedUser(userId: Long)(implicit ticket: Ticket): Future[User] = {
    try {
      assertAdmin
      userRepository.getById(userId) map (userOption => {
        if(userOption.isEmpty || userOption.get.key.isDefined) throw new Exception("No such user found")
        userOption.get
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  //TODO
  // def isUserByMail(String email): Boolean = {}

  //TODO
  // def isUserByUserName(String userName): Boolean = {}

  //TODO
  // def updateUserData()


  /**
   * Update a Users role.<br />
   * The role of the SYSTEM User can not be changed and no User can be promoted to SYSTEM.
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without ADMIN rights
   *
   * @param userId id of the User which role is changed
   * @param roleUpdate string value of the new role
   * @param ticket implicit authentication ticket
   * @return Future[Int]
   */
  def updateUserRole(userId: Long, roleUpdate: String)(implicit ticket: Ticket): Future[Int] = {
    try {
      assertAdmin
      val role = UserLogic.parseRole(roleUpdate)
      if (role == Role.SYSTEM) throw new Exception("Promotion to SYSTEM is not possible")
      userRepository.getById(userId) flatMap (userOption => {
        if (userOption.isEmpty) throw new Exception("No such user found")
        val user = userOption.get
        if (user.role == Role.SYSTEM) throw new Exception("The SYSTEM user can not be changed")
        val userUpdate = User(user.id, user.username, user.email, user.password, role, user.key, user.accepted, user.enabled)
        userRepository.update(userUpdate)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }


}
