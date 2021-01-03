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

import javax.inject.{Inject, Singleton}
import middleware.{AuthenticatedRequest, Authentication, AuthenticationFilter}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import modules.user.service.GroupService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * AccountController responsible for account actions and information.
 *
 * @param cc                 injected ControllerComponents
 * @param withAuthentication injected AuthenticationFilter
 * @param groupService       injected GroupService
 */
@Singleton
class AccountController @Inject()(cc: ControllerComponents, withAuthentication: AuthenticationFilter, groupService: GroupService)
  extends AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Endpoint to get the account page without overview information (just log out buttons).<br />
   * This endpoint should not be accessed directly but is a fallback for the actual overview endpoint.
   *
   * @return account overview html
   */
  def index: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      val error = request.flash.get("error")
      Future.successful(Ok(views.html.container.user.account.account_overview_fallback(error)))
    }
  }

  /**
   * Endpoint to get the account overview page.<br />
   * Provides all information at once the user needs to understand his rights an accesses.
   *
   * @return account overview html
   */
  def getAccountOverview: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      for {
        groups <- groupService.getGroupsOfUser
      } yield {
        val error = request.flash.get("error")
        Ok(views.html.container.user.account.account_overview(groups, error))
      }
    } recoverWith {
      case e =>
        logger.error(e.getMessage, e)
        Future.successful(Redirect(routes.ApplicationController.index()).flashing("error" -> e.getMessage))
    }
  }

}