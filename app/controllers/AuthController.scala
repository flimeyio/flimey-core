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

import modules.auth.formdata.{AuthenticateForm, LoginForm}
import modules.auth.service.AuthService
import com.google.inject.{Inject, Singleton}
import middleware.{AuthenticatedRequest, Authentication, AuthenticationFilter}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import modules.user.service.UserService

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
class AuthController @Inject()(cc: ControllerComponents,
                               withAuthentication: AuthenticationFilter,
                               authService: AuthService,
                               userService: UserService) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Endpoint to provide the login page.<br />
   * If the client has already a session set, the request is redirected to the ApplicationControllers overview endpoint.
   * (Even if the session is invalid, there will be another redirect to the login if required)
   *
   * @return login html page or overview redirect
   */
  def getLoginPage: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (getSessionKey(request).isDefined) {
      Redirect(routes.ApplicationController.overview())
    } else {
      //this error is flashed by the AuthenticationFilter if an unauthenticated request was detected
      val error = request.flash.get("error")

      Ok(views.html.container.auth.login_container(LoginForm.form.fill(LoginForm.Data("", "")), error))
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
        Future.successful(Ok(views.html.container.auth.login_container(errorForm, None)))
      },
      data => {
        authService.createSession(data.email, data.password) flatMap (sessionKey => {
          grantAuthentication(Redirect(routes.ApplicationController.overview()), sessionKey)
        }) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Ok(views.html.container.auth.login_container(LoginForm.form.fill(data), Option(e.getMessage))))
        }
      })
  }

  /**
   * Endpoint to get the authentication page with authentication form.<br />
   * If the User is already logged in, the request is redirected to the overview page.
   *
   * @return authentication container with empty form
   */
  def getAuthenticatePage: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (getSessionKey(request).isDefined) {
      Redirect(routes.ApplicationController.overview())
    } else {
      //This error is not flashed yet, but may be flashed by the login logic, if the user is not authenticated but tries to log in
      val error = request.flash.get("error")

      Ok(views.html.container.auth.authenticate_container(AuthenticateForm.form.fill(AuthenticateForm.Data("", "", "", agree = false)), error))
    }
  }

  /**
   * Endpoint to authenticate an invited (unauthenticated) User.<br />
   * The User must provide the correct authentication key as well as a new email and password.
   * If this operation finishes successfully, the User is able to log in.
   *
   * @return authentication page with form errors or redirect to login page
   */
  def authenticate: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    AuthenticateForm.form.bindFromRequest fold(
      errorForm => {
        Future.successful(Ok(views.html.container.auth.authenticate_container(errorForm, None)))
      },
      data => {
        userService.authenticateUser(data.key, data.email, data.password, data.agree) map (_ => {
          Redirect(routes.AuthController.login())
        }) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Ok(views.html.container.auth.authenticate_container(AuthenticateForm.form.fill(data), Option(e.getMessage))))
        }
      })
  }

  /**
   * Endpoint to log out.<br />
   * This will remove the currently active session from the client device and destroys the associated database representation.<br />
   * If the global flag is set, the logout will remove ALL sessions of the User, i.e. perform a log out on all devices.
   * Afterwards the User will be redirected to the login page.
   *
   * @return redirect to login with remove_session header set
   */
  def logout(all: Option[Boolean]): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      authService.deleteSession(all) flatMap { _ =>
        removeAuthentication(Redirect(routes.AuthController.login()))
      } recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ApplicationController.overview()).flashing("error" -> e.getMessage))
      }
    }
  }

}
