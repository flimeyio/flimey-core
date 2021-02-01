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
import modules.auth.model.Ticket
import modules.core.formdata.{NewEntityForm, SelectTypeForm}
import modules.subject.service.{CollectionService, ModelCollectionService}
import modules.user.service.GroupService
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Controller to provide all required endpoints to manage [[modules.subject.model.Collection Collections]]
 *
 * @param cc injected ControllerComponents (provides methods and implicits)
 * @param withAuthentication injected [[middleware.AuthenticationFilter AuthenticationFilter]] to handle session verification
 * @param collectionService injected [[modules.subject.service.CollectionService CollectionService]]
 * @param modelCollectionService injected [[modules.subject.service.ModelCollectionService ModelCollectionService]]
 * @param groupService injected [[modules.user.service.GroupService GroupService]]
 */
@Singleton
class CollectionController @Inject()(cc: ControllerComponents, withAuthentication: AuthenticationFilter,
                                     collectionService: CollectionService, modelCollectionService: ModelCollectionService,
                                     groupService: GroupService) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Endpoint to show the [[modules.subject.model.Collection Collection]] overview page.
   * <p> Depending on the flashed error, different configurations are shown:
   * <p> 1. There is an "recursive_error" (an error that will lead to recursive redirect), the redirect is stopped and
   * an empty collection page is returned
   * <p> 2. In all other cases the getCollections() is redirected, with or without not recursive error
   *
   * @return asset overview page
   */
  def index: Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        val error = request.flash.get("error")
        val recursiveError = request.flash.get("recursive_error")
        if (recursiveError.isEmpty) {
          if (error.isDefined) {
            Future.successful(Redirect(routes.CollectionController.getCollections()).flashing("error" -> error.get))
          } else {
            Future.successful(Redirect(routes.CollectionController.getCollections()))
          }
        } else {
          Future.successful(Ok(views.html.container.subject.collection_overview(Seq(), Seq(), recursiveError)))
        }
      }
    }

  /**
   * Right now, this endpoint ignores the query and just redirects to getCollections()
   *
   * @return redirect to getCollections()
   */
  //FIXME ############################################################################################################
  def searchCollections: Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        Future.successful(Redirect(routes.CollectionController.getCollections()))
      }
    }

  /**
   * Right now, this endpoint ignores the query and just redirects to getCollections()
   *
   * @return redirect to getCollections()
   */
  //FIXME ############################################################################################################
  def findByQuery: Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        Future.successful(Redirect(routes.CollectionController.getCollections()))
      }
    }

  /**
   * Endpoint to get all [[modules.subject.model.Collection Collection]] the requesting user can access.
   *
   * @param typeSelector  FIXME: this is ignored right now
   * @param groupSelector FIXME: this is ignored right now
   * @return collection overview page
   */
  def getCollections(typeSelector: Option[String] = None, groupSelector: Option[String] = None):
  Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      collectionService.getCollectionComplex(typeSelector, groupSelector) map (collectionComplex => {
        val error = request.flash.get("error")
        Ok(views.html.container.subject.collection_overview(collectionComplex.collectionTypes, collectionComplex.collections, error))
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.CollectionController.index()).flashing("recursive_error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to redirect to a new collection editor of the specified type (by a post request via form submit)
   * redirects to the equivalent get endpoint.
   *
   * @return redirect to getNewCollectionEditor or form with errors
   */
  def requestNewCollectionEditor(): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        SelectTypeForm.form.bindFromRequest fold(
          errorForm => {
            Future.successful(Redirect(routes.CollectionController.index()).flashing("error" -> "Invalid Collection Type input"))
          },
          data => {
            val collectionTypeValue = data.value
            modelCollectionService.getTypeByValue(collectionTypeValue) map (collectionType => {
              if (collectionType.isEmpty) throw new Exception("No such Collection Type found")
              Redirect(routes.CollectionController.getNewCollectionEditor(collectionType.get.id))
            }) recoverWith {
              case e =>
                logger.error(e.getMessage, e)
                Future.successful(Redirect(routes.CollectionController.index()).flashing("error" -> e.getMessage))
            }
          })
      }
    }

  /**
   * Endpoint to get an editor to create new [[modules.subject.model.Collection Collections]].
   * <p> The Editor will only accept Collections  of the previously selected Entity(Collection)Type.
   *
   * @return new collection editor
   */
  def getNewCollectionEditor(typeId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        Future.successful(Redirect(routes.CollectionController.index()).flashing("error" -> "Not Implemented yet"))
        val newEntityForm = NewEntityForm.form.fill(NewEntityForm.Data(Seq(), Seq(), Seq(), Seq()))
        val error = request.flash.get("error")
        val success = request.flash.get("success")
        newCollectionEditorFactory(typeId, newEntityForm, error, success)
      }
    }

  /**
   * Endpoint to add a new [[modules.subject.model.Collection Collection]].<br />
   * The Collection must be of the selected Collection EntityType.<br />
   * The incoming form data seq must be in the same order as the previously sent property keys.
   *
   * @return new collection editor (clean or with errors)
   */
  def addNewCollection(typeId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        Future.successful(Redirect(routes.CollectionController.index()).flashing("error" -> "Not Implemented yet"))
        NewEntityForm.form.bindFromRequest fold(
          errorForm => newCollectionEditorFactory(typeId, errorForm),
          data => {
            collectionService.addCollection(typeId, data.values, data.maintainers, data.editors, data.viewers) map (_ => {
              Redirect(routes.CollectionController.getNewCollectionEditor(typeId)).flashing("success" -> "Collection successfully created")
            }) recoverWith {
              case e =>
                logger.error(e.getMessage, e)
                val newEntityForm = NewEntityForm.form.fill(data)
                newCollectionEditorFactory(typeId, newEntityForm, Option(e.getMessage))
            }
          })
      }
    }

  /**
   * Helper function to build a 'new collection editor' view based on different configuration parameters.
   *
   * @param typeId  id of the EntityType
   * @param form    NewEntityForm, which can be already filled
   * @param errmsg  optional error message
   * @param succmsg optional positive message
   * @param request implicit request context
   * @return new entity editor result future (view)
   */
  private def newCollectionEditorFactory(typeId: Long, form: Form[NewEntityForm.Data], errmsg: Option[String] = None, succmsg: Option[String] = None)
                                        (implicit request: Request[AnyContent], ticket: Ticket): Future[Result] = {
    for {
      groups <- groupService.getAllGroups
      typeData <- modelCollectionService.getCompleteType(typeId)
    } yield {
      val (assetType, constraints) = typeData
      Ok(views.html.container.subject.new_collection_editor(assetType,
        collectionService.getCollectionPropertyKeys(constraints),
        collectionService.getObligatoryPropertyKeys(constraints),
        groups,
        form, errmsg, succmsg))
    }
  } recoverWith {
    case e =>
      logger.error(e.getMessage, e)
      Future.successful(Redirect(routes.CollectionController.index()).flashing("error" -> e.getMessage))
  }

}


