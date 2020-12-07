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

package controllers

import formdata.user.NewUserForm
import javax.inject.{Inject, Singleton}
import middleware.{AuthenticatedRequest, Authentication, AuthenticationFilter}
import model.user.Role
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.group.GroupService
import services.user.UserService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * ManagementController responsible for all administrative actions like user and group management.
 *
 * @param cc injected ControllerComponents
 * @param withAuthentication injected AuthenticationFilter
 * @param userService injected UserService
 * @param groupService injected GroupService
 */
@Singleton
class ManagementController @Inject()(cc: ControllerComponents, withAuthentication: AuthenticationFilter,
                                     userService: UserService, groupService: GroupService) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Helper method to redirect if no sufficient rights are found. <br />
   * This redirects to the Application overview (where another redirection happens in case of missing session).
   * <br />
   * Other endpoints of the ManagementController call this method in case of invalid rights.
   *
   * @return overview redirect
   */
  def redirectWithNoRights: Future[Result] = Future.successful(
    Redirect(routes.ApplicationController.overview()).flashing("error" -> "No rights to access the admin area"))


  /**
   * Endpoint tho provide the administration landing page.<br />
   * This call requires ADMIN rights.
   *
   * @return administration overview html
   */
  def index: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      //In this plain get controller, the rights must be checked here, because no service call is executed
      if (!Role.isAtLeastAdmin(ticket.authSession.role)) {
        redirectWithNoRights
      } else {
        val error = request.flash.get("error")
        Future.successful(Ok(views.html.container.user.management.management_overview(error)))
      }
    }
  }

  /**
   * Endpoint to get display all Users with open authentication.<br />
   * The returned data can be used to access the authentication keys and to delete invitations.
   *
   * @return invitation management html
   */
  def getInvitedUsers: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      userService.getAllInvitedUsers() map (invitedUsers => {
        val error = request.flash.get("error")
        Ok(views.html.container.user.management.management_invitations(invitedUsers, error))
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.index()).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to get the admin page with open invitation form.<br />
   * Shows an additional error if one is in the flash scope (on redirect).
   * The form is always empty.
   *
   * @return new invitation page html
   */
  def getInvitationForm: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      //In this plain get controller, the rights must be checked here, because no service call is executed
      if (!Role.isAtLeastAdmin(ticket.authSession.role)) {
        redirectWithNoRights
      } else {
        val emptyForm = NewUserForm.form.fill(NewUserForm.Data("","WORKER"))
        val error = request.flash.get("error")
        Future.successful(Ok(views.html.container.user.management.management_invitations_new(emptyForm, error)))
      }
    }
  }

  /**
   * Endpoint to add a new invitation/Invite a new User.<br />
   * Invalid form data leads to a returned form page with error messages.
   * <br />
   * Returns an empty new invitation form on success, else the filled form with error messages.
   *
   * @return new invitation page html
   */
  def postNewInvitation: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      NewUserForm.form.bindFromRequest fold(
        errorForm => {
          Future.successful(Ok(views.html.container.user.management.management_invitations_new(errorForm)))
        },
        data => {
          userService.createUser(data.userName, data.role) map (_ => {
            Redirect(routes.ManagementController.getInvitationForm())
          }) recoverWith {
            case e =>
              logger.error(e.getMessage, e)
              Future.successful(Ok(views.html.container.user.management.management_invitations_new(NewUserForm.form.fill(data), Option(e.getMessage))))
          }
        })
    }
  }

  /**
   * Endpoint to delete an Invitation.<br />
   * After deletion, the invited User can no longer authenticate.
   *
   * @param userId id of the invitation (user) to delete
   * @return invitation management html
   */
  def deleteInvitation(userId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      userService.deleteUser(userId) map (_ => {
        Redirect(routes.ManagementController.getInvitedUsers())
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.getInvitedUsers()).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   *
   * @return
   */
  //FIXME
  def getGroups: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      if (!Role.isAtLeastAdmin(ticket.authSession.role)) {
        val error = request.flash.get("error")
        redirectWithNoRights
      } else {
        Future.successful(Redirect(routes.ManagementController.index()).flashing("error" -> "Not implemented yet!"))
      }
    }
  }

  /**
   *
   * @return
   */
  //FIXME
  def postNewGroup: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      if (!Role.isAtLeastAdmin(ticket.authSession.role)) {
        redirectWithNoRights
      } else {
        Future.successful(Redirect(routes.ManagementController.index()).flashing("error" -> "Not implemented yet!"))
      }
    }
  }

  /**
   *
   * @param groupId
   * @return
   */
  //FIXME
  def deleteGroup(groupId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      if (!Role.isAtLeastAdmin(ticket.authSession.role)) {
        redirectWithNoRights
      } else {
        Future.successful(Redirect(routes.ManagementController.index()).flashing("error" -> "Not implemented yet!"))
      }
    }
  }

  /**
   *
   * @param email
   * @param groupId
   * @return
   */
  //FIXME
  def addUserToGroup(email: Long, groupId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      if (!Role.isAtLeastAdmin(ticket.authSession.role)) {
        redirectWithNoRights
      } else {
        Future.successful(Redirect(routes.ManagementController.index()).flashing("error" -> "Not implemented yet!"))
      }
    }
  }

  /**
   *
   * @param email
   * @param groupId
   * @return
   */
  //FIXME
  def deleteUserFromGroup(email: Long, groupId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      if (!Role.isAtLeastAdmin(ticket.authSession.role)) {
        redirectWithNoRights
      } else {
        Future.successful(Redirect(routes.ManagementController.index()).flashing("error" -> "Not implemented yet!"))
      }
    }
  }

  /**
   *
   * @param email
   * @param role
   * @return
   */
  //FIXME
  def postUserRole(email: Long, role: String): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      if (!Role.isAtLeastAdmin(ticket.authSession.role)) {
        redirectWithNoRights
      } else {
        Future.successful(Redirect(routes.ManagementController.index()).flashing("error" -> "Not implemented yet!"))
      }
    }
  }

}