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
import middleware.Authentication
import model.user.User
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._

@Singleton
class AuthController @Inject()(cc: ControllerComponents) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  def getLoginPage(msg: Option[String] = None): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.container.auth.login_container(msg))
  }

  def login: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    withAuthentication (Ok(views.html.app()()()), User(0, "", "", "", "", "", true, true), Seq())
  }

  def getAuthenticatePage(msg: Option[String] = None): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.container.auth.authenticate_container(msg))
  }

  def authenticate: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Redirect(routes.AuthController.getLoginPage())
  }

  def logout: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    withoutAuthentication (Ok(views.html.app()()()))
  }

}
