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
import user.model
import user.model.User
import user.repository.UserRepository
import util.assertions.RoleAssertion

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic for User and Account Management.
 * This class is normally used by dependency injection inside controller endpoints.
 *
 * @param userRepository injected db interface for User entities.
 */
class UserService @Inject()(userRepository: UserRepository) extends RoleAssertion {

  /**
   * Create a new User (invitation).<br />
   * The User is created with an unique username and a role.
   * After such a new User is created, he can not log in until the account is authenticated in a separate step.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param userName unique visible name of the User
   * @param role represents rights see [[model.user.Role]] management doc for more information
   * @param ticket implicit authentication ticket
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
   * <br />
   * A User can only be deleted by himself or an admin User.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param userId of the User to delete
   * @param ticket implicit authentication ticket
   * @return
   */
  def deleteUser(userId: Long)(implicit ticket: Ticket): Future[Unit] = {
    //only admin users or the User itself can delete an account.
    if(ticket.authSession.userId != userId){
     assertAdmin
    }
    //TODO
    Future.successful()
  }

  /**
   * Authenticate a invited User.<br />
   * Fills the missing fields of a previously invited User and enables the login.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   * <br />
   * Fails without ADMIN rights.
   *
   * @param key authentication key (generated on createUser)
   * @param email unique email
   * @param password login password
   * @param agree agreement to terms and conditions
   * @return
   */
  def authenticateUser(key: String, email: String, password: String, agree: Boolean): Future[Int] = {
    try {
      if (!agree) throw new Exception("You must agree to the Terms & Conditions to create an account!")
      userRepository.getByKey(key) flatMap (userOption => {
        if (userOption.isEmpty) throw new Exception("Invalid key!")
        val credentialStatus = UserLogic.isValidAuthenticationData(email, password)
        if (!credentialStatus.valid) credentialStatus.throwError
        val userUpdate = UserLogic.updateCredentialsOnAuthentication(userOption.get, email, password)
        //FIXME: add User to the public group
        userRepository.update(userUpdate)
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

  //TODO
  // def isUserByMail(String email): Boolean = {}

  //TODO
  // def isUserByUserName(String userName): Boolean = {}

  //TODO
  // def updateUserData()

  //TODO
  // def updateUserRole()

  //TODO
  // def deleteUser()

}
