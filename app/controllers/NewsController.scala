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
import modules.news.service.NewsService
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Controller to provide all required endpoints to manage [[modules.news.model.NewsEvent NewsEvents]].
 *
 * @param cc                      injected ControllerComponents (provides methods and implicits)
 * @param withAuthentication      injected [[middleware.AuthenticationFilter AuthenticationFilter]] to handle session verification
 */
@Singleton
class NewsController @Inject()(cc: ControllerComponents,
                               withAuthentication: AuthenticationFilter,
                               newsService: NewsService) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Endpoint to get all latest [[modules.news.model.NewsEvent NewsEvents]].
   *
   * @return overview page with the 100 latest news events
   */
  def index: Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        newsService.getFeed(100) map (extendedNewsEvents => {
          val error = request.flash.get("error")
          Ok(views.html.container.overview.overview(extendedNewsEvents, error))
        }) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Ok(views.html.container.overview.overview(Seq(), Option(e.getMessage))))
        }
      }
    }

}