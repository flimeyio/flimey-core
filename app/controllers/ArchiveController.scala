/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2021 Karl Kegel
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
import modules.core.formdata.StringQueryForm
import modules.subject.service.CollectionService
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Controller to provide all required endpoints to manage archived [[modules.subject.model.Collection Collections]]
 *
 * @param cc                     injected ControllerComponents (provides methods and implicits)
 * @param withAuthentication     injected [[middleware.AuthenticationFilter AuthenticationFilter]] to handle session verification
 * @param collectionService      injected [[modules.subject.service.CollectionService CollectionService]]
 */
@Singleton
class ArchiveController @Inject()(cc: ControllerComponents,
                                  withAuthentication: AuthenticationFilter,
                                  collectionService: CollectionService) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * TODO add doc
   *
   * @param query
   * @return
   */
  def index(query: String): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        if (!query.isEmpty) {
          collectionService.findArchivedCollection(query) map (collections => {
            Ok(views.html.container.subject.collection_archive(collections, query))
          }) recoverWith {
            case e =>
              logger.error(e.getMessage, e)
              Future.successful(Ok(views.html.container.subject.collection_archive(Seq(), query, Some(e.getMessage))))
          }
        } else {
          val error = request.flash.get("error")
          Future.successful(Ok(views.html.container.subject.collection_archive(Seq(), "", error)))
        }
      }
    }

  def query(): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        StringQueryForm.form.bindFromRequest fold(
          errorForm => {
            Future.successful(Redirect(routes.ArchiveController.index("")).flashing("error" -> "Invalid search input"))
          },
          data => {
            val query = data.query
            Future.successful(Redirect(routes.ArchiveController.index(query)))
          })
      }
    }

}