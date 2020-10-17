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

import com.google.inject.{Inject, Singleton}
import formdata.auth.LoginForm
import middleware.{AuthenticatedRequest, Authentication, AuthenticationFilter}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.auth.AuthService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The AuthController responsible for all endpoints regarding authentication and session management.
 *
 * @param cc                 injected ControllerComponents
 * @param withAuthentication injected AuthenticationFilter
 * @param authService        injected AuthenticationService
 */
@Singleton
class AuthController @Inject()(cc: ControllerComponents, withAuthentication: AuthenticationFilter, authService: AuthService) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Endpoint to provide the login page.<br />
   * If the client has already a session set, the request is redirected to the ApplicationControllers overview endpoint.
   * (Even if the session is invalid, there will be another redirect to the login if required)
   *
   * @param msg optional error message
   * @return login html page or overview redirect
   */
  def getLoginPage(msg: Option[String] = None): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (getSessionKey(request).isDefined) {
      Redirect(routes.ApplicationController.overview())
    } else {
      Ok(views.html.container.auth.login_container(LoginForm.Data("", ""), msg))
    }
  }

  /**
   * Endpoint to perform the login operation.<br />
   * Reads the login form and generates the sessionKey which is send to the User as a new session cookie.<br />
   * If login is successful, the user will be redirected to the overview page.
   *
   * @return overview redirect (with session) or login with error dialog
   */
  def login: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    LoginForm.form.bindFromRequest fold(
      errorForm => {
        Future.successful(Ok(views.html.container.auth.login_container(errorForm.get, Option(errorForm.errors.toString()))))
      },
      data => {
        authService.createSession(data.email, data.password) flatMap (sessionKey => {
          grantAuthentication(Redirect(routes.ApplicationController.overview()), sessionKey)
        }) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Ok(views.html.container.auth.login_container(data, Option(e.getMessage))))
        }
      })
  }

  //TODO
  def getAuthenticatePage(msg: Option[String] = None): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.container.auth.authenticate_container(msg))
  }

  //TODO
  def authenticate: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Redirect(routes.AuthController.getLoginPage())
  }

  /**
   * Endpoint to log out.<br />
   * This will remove the currently active session from the client device and destroys the associated database
   * representation.<br />
   * Afterwards the User will be redirected to the login page.
   *
   * @return redirect to login without remove_session header set
   */
  def logout: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      authService.deleteSession() flatMap { _ =>
        removeAuthentication(Redirect(routes.AuthController.login()))
      } recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          //FIXME display page where logout is visible... or overview with flash error message
          Future.successful(Redirect(routes.ApplicationController.overview(/*Option(e.getMessage)*/)))
      }
    }
  }

}
