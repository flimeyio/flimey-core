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

import javax.inject._
import middleware.{Authentication, AuthenticationFilter}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._

/**
 * The ApplicationController responsible for the page index and overview endpoints.
 *
 * @param cc                 injected ControllerComponents
 * @param withAuthentication injected AuthenticationAction
 */
@Singleton
class ApplicationController @Inject()(cc: ControllerComponents, withAuthentication: AuthenticationFilter) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Page index Endpoint. Redirects to the login page.
   *
   * @return redirection to login action of the AuthController
   */
  def index: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Redirect(routes.AuthController.login())
  }

}
