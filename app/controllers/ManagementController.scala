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

import auth.service.AuthService
import javax.inject.{Inject, Singleton}
import middleware.{AuthenticatedRequest, Authentication, AuthenticationFilter}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import user.formdata._
import user.model.Role
import user.service.{GroupService, UserService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * ManagementController responsible for all administrative actions like user and group management.
 *
 * @param cc                 injected ControllerComponents
 * @param withAuthentication injected AuthenticationFilter
 * @param userService        injected UserService
 * @param groupService       injected GroupService
 * @param authService        injected AuthService
 */
@Singleton
class ManagementController @Inject()(cc: ControllerComponents,
                                     withAuthentication: AuthenticationFilter,
                                     userService: UserService,
                                     groupService: GroupService,
                                     authService: AuthService) extends
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
   * <p> required admin rights are checked directly here.
   *
   * @return new invitation page html
   */
  def getInvitationForm: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      //In this plain get controller, the rights must be checked here, because no service call is executed
      if (!Role.isAtLeastAdmin(ticket.authSession.role)) {
        redirectWithNoRights
      } else {
        val emptyForm = NewUserForm.form.fill(NewUserForm.Data("", "WORKER"))
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
   * Endpoint to delete a User.<br />
   * After deletion, the User can no longer log in and is immediately logged out from all devices.<br />
   * <p> This endpoint is accessable with ADMIN and WORKER rights.
   *
   * @param userId id of the invitation (user) to delete
   * @return invitation management html
   */
  def deleteUser(userId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      userService.deleteUser(userId) flatMap (_ => {
        authService.deleteSession(Option(true), Option(userId)) map (_ => {
          if (userId == ticket.authSession.userId) {
            Redirect(routes.AuthController.login()).flashing("error" -> "Account was deleted successfully")
          } else {
            Redirect(routes.ManagementController.getUsers())
          }
        })
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.getUserEditor(userId)).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to get all (authenticated) Users.<br />
   *
   * @return management view html with User list
   */
  def getUsers: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      userService.getAllAuthenticatedUsers map (users => {
        val error = request.flash.get("error")
        Ok(views.html.container.user.management.management_users(users, error))
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.index()).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to get all Groups.<br />
   *
   * @return management view html with group list
   */
  def getGroups: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      groupService.getAllGroups map (groups => {
        val error = request.flash.get("error")
        Ok(views.html.container.user.management.management_groups(groups, error))
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.index()).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to get the admin page with open new Group form.<br />
   * The form is empty by default.<br />
   * <p> required admin rights are checked directly here.
   *
   * @return management page html with new Group form
   */
  def getNewGroupForm: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      //In this plain get controller, the rights must be checked here, because no service call is executed
      if (!Role.isAtLeastAdmin(ticket.authSession.role)) {
        redirectWithNoRights
      } else {
        val emptyForm = NewGroupForm.form.fill(NewGroupForm.Data(""))
        val error = request.flash.get("error")
        Future.successful(Ok(views.html.container.user.management.management_groups_new(emptyForm, error)))
      }
    }
  }

  /**
   * Endpoint to post a new Group.<br />
   * Invalid form data leads to a returned form page with error messages.
   * <br />
   * Redirects to group overview on success, else the filled form with error messages.
   *
   * @return new group page or group overview on success
   */
  def postNewGroup: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      NewGroupForm.form.bindFromRequest fold(
        errorForm => {
          Future.successful(Ok(views.html.container.user.management.management_groups_new(errorForm)))
        },
        data => {
          groupService.addGroup(data.groupName) map (_ => {
            Redirect(routes.ManagementController.getGroups())
          }) recoverWith {
            case e =>
              logger.error(e.getMessage, e)
              Future.successful(Ok(views.html.container.user.management.management_groups_new(NewGroupForm.form.fill(data), Option(e.getMessage))))
          }
        })
    }
  }

  /**
   * Endpoint to delete a Group.<br />
   *
   * @param groupId id of the Group to delete
   * @return redirect to getGroups (Group overview management html)
   */
  def deleteGroup(groupId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      groupService.deleteGroup(groupId) map (_ => {
        Redirect(routes.ManagementController.getGroups())
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.getGroups()).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to get the Group editor with all members and the new member form.<br />
   *
   * @return management view html with group editor
   */
  def getGroupEditor(groupId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      for {
        group <- groupService.getGroup(groupId)
        members <- groupService.getUsersOfGroup(groupId)
      } yield {
        val error = request.flash.get("error")
        val emptyForm = NewGroupMemberForm.form.fill(NewGroupMemberForm.Data(""))
        Ok(views.html.container.user.management.management_group_editor(group, members, emptyForm, error))
      }
    } recoverWith {
      case e =>
        logger.error(e.getMessage, e)
        Future.successful(Redirect(routes.ManagementController.getGroups()).flashing("error" -> e.getMessage))
    }
  }

  /**
   * Endpoint to add a new member (User) to a Group.<br />
   * Invalid form data leads to a returned editor page with empty form and error messages.
   *
   * @param groupId id of the Group
   * @return redirect to group editor
   */
  def addUserToGroup(groupId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      NewGroupMemberForm.form.bindFromRequest fold(
        errorForm => {
          Future.successful(Redirect(routes.ManagementController.getGroupEditor(groupId)).flashing("error" -> "Invalid e-mail address"))
        },
        data => {
          groupService.addMembership(groupId, data.userMail) map (_ => {
            Redirect(routes.ManagementController.getGroupEditor(groupId))
          }) recoverWith {
            case e =>
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.ManagementController.getGroupEditor(groupId)).flashing("error" -> e.getMessage))
          }
        })
    }
  }

  /**
   * Delete a member (User) from a Group.<br />
   *
   * @param groupId id of the Group
   * @param userId  id of the User to remove
   * @return redirect to group editor
   */
  def deleteUserFromGroup(groupId: Long, userId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      groupService.deleteMembership(groupId, userId) map (_ => {
        Redirect(routes.ManagementController.getGroupEditor(groupId))
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.getGroupEditor(groupId)).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to get the GroupRelation (Viewer) editor with all viewers and the new viewer form.
   *
   * @return management view html with group relation editor
   */
  def getGroupRelationEditor(groupId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      groupService.getGroup(groupId) flatMap (group => {
        groupService.getFirstClassGroupViewers(groupId) map (groupViewerRelation => {
          val error = request.flash.get("error")
          val emptyForm = NewGroupViewerForm.form.fill(NewGroupViewerForm.Data("", ""))
          Ok(views.html.container.user.management.management_group_relation_editor(group, groupViewerRelation, emptyForm, error))
        })
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.getGroups()).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to add a new member (User) to a Group.<br />
   * Invalid form data leads to a returned editor page with empty form and error messages.
   *
   * @param groupId id of the Group
   * @return redirect to group editor
   */
  def postNewGroupRelation(groupId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      NewGroupViewerForm.form.bindFromRequest fold(
        errorForm => {
          groupService.getGroup(groupId) flatMap (group => {
            groupService.getFirstClassGroupViewers(groupId) map (groupViewerCombinator => {
              Ok(views.html.container.user.management.management_group_relation_editor(group, groupViewerCombinator, errorForm, None))
            })
          })
        },
        data => {
          groupService.addRelation(groupId, data.viewerName, data.viewerRole) map (_ => {
            Redirect(routes.ManagementController.getGroupRelationEditor(groupId))
          }) recoverWith {
            case e =>
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.ManagementController.getGroupRelationEditor(groupId)).flashing("error" -> e.getMessage))
          }
        })
    }
  }

  /**
   * Delete Group(Viewer) relation.
   *
   * @param groupId  id of the viewed Group
   * @param viewerId id of the viewing Group
   * @return redirect to group editor
   */
  def deleteGroupRelation(groupId: Long, viewerId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      groupService.removeRelation(groupId, viewerId) map (_ => {
        Redirect(routes.ManagementController.getGroupRelationEditor(groupId))
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.getGroupRelationEditor(groupId)).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to get the Group editor with all members and the new member form.<br />
   *
   * @return management view html with group editor
   */
  def getUserEditor(userId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      userService.getAuthenticatedUser(userId) map (user => {
        val error = request.flash.get("error")
        val emptyForm = EditUserForm.form.fill(EditUserForm.Data(""))
        Ok(views.html.container.user.management.management_user_editor(user, emptyForm, error))
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.getUsers()).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to update a User.<br />
   * This endpoint updates currently only the role.
   * The role update does not take effect until the updated User logs out and in again.
   *
   * @param userId id of the User to update
   * @return
   */
  def postUser(userId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      EditUserForm.form.bindFromRequest fold(
        errorForm => {
          userService.getAuthenticatedUser(userId) map (user => {
            Ok(views.html.container.user.management.management_user_editor(user, errorForm, None))
          })
        },
        data => {
          userService.updateUserRole(userId, data.role) map (_ => {
            Redirect(routes.ManagementController.getUserEditor(userId))
          }) recoverWith {
            case e =>
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.ManagementController.getUserEditor(userId)).flashing("error" -> e.getMessage))
          }
        })
    }
  }

  /**
   * Endpoint to perform a forced log out of a User on all his devices.<br />
   *
   * @param userId id of the User to log out
   * @return
   */
  def logUserOut(userId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      authService.deleteSession(Option(true), Option(userId)) map (_ => {
        if (userId == ticket.authSession.userId) {
          Redirect(routes.AuthController.login()).flashing("error" -> "Logged out successfully")
        } else {
          Redirect(routes.ManagementController.getUserEditor(userId))
        }
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ManagementController.getUserEditor(userId)).flashing("error" -> e.getMessage))
      }
    }
  }

}