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
import modules.core.formdata.{EntityForm, SelectValueForm}
import modules.subject.service.{CollectibleService, ModelCollectibleService}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Controller to provide all required endpoints to manage [[modules.subject.model.Collectible Collectibles]].
 *
 * @param cc                      injected ControllerComponents (provides methods and implicits)
 * @param withAuthentication      injected [[middleware.AuthenticationFilter AuthenticationFilter]] to handle session verification
 * @param collectibleService      injected [[modules.subject.service.CollectibleService CollectibleService]]
 * @param modelCollectibleService injected [[modules.subject.service.ModelCollectibleService ModelCollectibleService]]
 */
@Singleton
class CollectibleController @Inject()(cc: ControllerComponents,
                                      withAuthentication: AuthenticationFilter,
                                      collectibleService: CollectibleService,
                                      modelCollectibleService: ModelCollectibleService) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Endpoint to delete a [[modules.subject.model.Collectible Collectible]].
   * <p> The Collectible is deleted permanently and can not be restored!
   *
   * @param collectionId  id of the parent [[modules.subject.model.Collection Collection]]
   * @param collectibleId id of the Collectible to delete
   * @return redirect to the parent Collection view
   */
  def deleteCollectible(collectionId: Long, collectibleId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        collectibleService.deleteCollectible(collectibleId) map (_ =>
          Redirect(routes.CollectionController.getCollection(collectionId))
          ) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(
              Redirect(routes.CollectibleController.getCollectibleEditor(collectionId, collectibleId)
              ).flashing("error" -> e.getMessage))
        }
      }
    }

  /**
   * Endpoint to get the [[modules.subject.model.Collectible Collectible]] editor with preloaded data.
   *
   * @param collectionId  id of the parent [[modules.subject.model.Collection Collection]]
   * @param collectibleId id of the Collectible to delete
   * @return collectible editor page with preloaded Collectible data
   */
  def getCollectibleEditor(collectionId: Long, collectibleId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        val error = request.flash.get("error")
        updateCollectibleEditorFactory(collectionId, collectibleId, None, error)
      }
    }

  /**
   * Endpoint to post (update) the data of the currently edited [[modules.subject.model.Collectible Collectible]].
   *
   * @param collectionId  id of the parent [[modules.subject.model.Collection Collection]]
   * @param collectibleId id of the Collectible to edit
   * @return collectible editor page with preloaded Collectible data with success or error message
   */
  def postCollectible(collectionId: Long, collectibleId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        EntityForm.form.bindFromRequest fold(
          errorForm => updateCollectibleEditorFactory(collectionId, collectibleId, Option(errorForm)),
          data => {
            collectibleService.updateCollectible(collectibleId, data.values) flatMap (_ => {
              updateCollectibleEditorFactory(collectionId, collectibleId, Some(EntityForm.form.fill(data)), None, Option("Changes saved successfully"))
            }) recoverWith {
              case e: Throwable =>
                logger.error(e.getMessage, e)
                val newEntityForm = EntityForm.form.fill(data)
                updateCollectibleEditorFactory(collectionId, collectibleId, Some(newEntityForm), Option(e.getMessage))
            }
          })
      }
    }

  /**
   * Endpoint to get the state editor of a [[modules.subject.model.Collectible Collectible]].
   *
   * @param collectionId  id of the parent [[modules.subject.model.Collection Collection]]
   * @param collectibleId id of the Collectible to edit
   * @return collectible state editor page
   */
  def getStateEditor(collectionId: Long, collectibleId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        collectibleService.getCollectible(collectibleId) map (extendedCollectible => {
          val error = request.flash.get("error")
          val succmsg = request.flash.get("succ")
          Ok(views.html.container.subject.collectible_state_graph(extendedCollectible.collectible, error, succmsg))
        }) recoverWith {
          case e: Throwable => Future.successful(Redirect(routes.CollectionController.getCollection(collectionId)).flashing("error" -> e.getMessage))
        }
      }
    }

  /**
   * Endpoint to update the state of a [[modules.subject.model.Collectible Collectible]].
   *
   * @param collectionId  id of the parent [[modules.subject.model.Collection Collection]]
   * @param collectibleId id of the Collectible to edit
   * @return Collectible state editor page (on error) or redirect to Collection overview
   */
  def postState(collectionId: Long, collectibleId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        SelectValueForm.form.bindFromRequest fold(
          errorForm => Future.successful(Redirect(routes.CollectibleController.getStateEditor(collectionId, collectibleId)).flashing("error" -> "Invalid form data")),
          data => {
            collectibleService.updateState(collectibleId, data.value) map (_ => {
              Redirect(routes.CollectibleController.getStateEditor(collectionId, collectibleId)).flashing("succ" -> "Changes saved successfully")
            }) recoverWith {
              case e: Throwable => Future.successful(Redirect(routes.CollectionController.getStateEditor(collectionId)).flashing("error" -> e.getMessage))
            }
          })
      }
    }

  /**
   * Endpoint to redirect to a new [[modules.subject.model.Collectible Collectible]] editor of the specified type
   * (by a post request via form submit)
   * <p> Redirects to the equivalent get endpoint with prepared typeId.
   *
   * @param collectionId id of the parent [[modules.subject.model.Collection Collection]]
   * @return redirect to getNewCollectibleEditor or form with errors
   */
  def requestNewCollectibleEditor(collectionId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        SelectValueForm.form.bindFromRequest fold(
          errorForm => {
            Future.successful(Redirect(routes.CollectionController.getCollection(collectionId)).flashing("error" -> "Invalid Collectible Type input"))
          },
          data => {
            val collectibleTypeValue = data.value
            modelCollectibleService.getTypeByValue(collectibleTypeValue) map (collectibleType => {
              if (collectibleType.isEmpty) Future.failed(new Exception("No such Collectible Type found"))
              Redirect(routes.CollectibleController.getNewCollectibleEditor(collectionId, collectibleType.get.id))
            })
          } recoverWith {
            case e =>
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.CollectionController.getCollection(collectionId)).flashing("error" -> e.getMessage))
          })
      }
    }

  /**
   * Endpoint to get an editor to create new [[modules.subject.model.Collectible Collectibles]].
   * <p> The Editor will only accept Collectibles of the previously selected [[modules.core.model.EntityType EntityType]].
   *
   * @param collectionId id of the parent [[modules.subject.model.Collection Collection]]
   * @return new collectible editor page
   */
  def getNewCollectibleEditor(collectionId: Long, typeId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        val newEntityForm = EntityForm.form.fill(EntityForm.Data(Seq(), Seq(), Seq(), Seq()))
        val error = request.flash.get("error")
        val success = request.flash.get("success")
        newCollectibleEditorFactory(collectionId, typeId, newEntityForm, error, success)
      }
    }

  /**
   * Endpoint to add a new [[modules.subject.model.Collectible Collectible]].
   * <p> The Collectible must be of the selected Collectible [[modules.core.model.EntityType EntityType]].
   * <p> The incoming form data seq must be in the same order as the previously sent property keys.
   * <p> The parent [[modules.subject.model.Collection Collection]] must support the child collectible.
   *
   * @see [[modules.subject.service.CollectibleService#addCollectible]]
   * @param collectionId id of the parent [[modules.subject.model.Collection Collection]]
   * @return new collectible editor (clean or with errors)
   */
  def addNewCollectible(collectionId: Long, typeId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        EntityForm.form.bindFromRequest fold(
          errorForm => newCollectibleEditorFactory(collectionId, typeId, errorForm),
          data => {
            collectibleService.addCollectible(collectionId, typeId, data.values) map (_ => {
              Redirect(routes.CollectibleController.getNewCollectibleEditor(collectionId, typeId)).flashing("success" -> "Collectible successfully created")
            }) recoverWith {
              case e =>
                logger.error(e.getMessage, e)
                val newEntityForm = EntityForm.form.fill(data)
                newCollectibleEditorFactory(collectionId, typeId, newEntityForm, Option(e.getMessage))
            }
          })
      }
    }

  /**
   * Helper function to build a 'new collectible editor' view based on different configuration parameters.
   *
   * @param collectionId id of the parent [[modules.subject.model.Collection Collection]]
   * @param typeId       id of the [[modules.core.model.EntityType EntityType]]
   * @param form         NewEntityForm, which can be already filled
   * @param errmsg       optional error message
   * @param succmsg      optional positive message
   * @param request      implicit request context
   * @return new entity editor result future (view)
   */
  private def newCollectibleEditorFactory(collectionId: Long, typeId: Long, form: Form[EntityForm.Data], errmsg: Option[String] = None,
                                          succmsg: Option[String] = None)(
                                           implicit request: Request[AnyContent], ticket: Ticket): Future[Result] = {
    modelCollectibleService.getCompleteType(typeId) map (typeData => {
      val (collectibleType, constraints) = typeData
      Ok(views.html.container.subject.new_collectible_editor(collectionId,
        collectibleType,
        collectibleService.getCollectiblePropertyKeys(constraints),
        collectibleService.getObligatoryPropertyKeys(constraints),
        form, errmsg, succmsg))
    })
  } recoverWith {
    case e =>
      logger.error(e.getMessage, e)
      Future.successful(Redirect(routes.CollectionController.getCollection(collectionId)).flashing("error" -> e.getMessage))
  }

  /**
   * Helper function to build a 'collectible editor' view based on different configuration parameters.
   *
   * @param collectionId  id of the parent [[modules.subject.model.Collection Collection]]
   * @param collectibleId id of the [[modules.subject.model.Collectible Collectible]] to edit
   * @param form          optional prepared form data
   * @param msg           optional error message
   * @param request       implicit request context
   * @return collection editor page
   */
  private def updateCollectibleEditorFactory(collectionId: Long, collectibleId: Long, form: Option[Form[EntityForm.Data]],
                                             msg: Option[String] = None, successMsg: Option[String] = None)(
                                              implicit request: Request[AnyContent], ticket: Ticket): Future[Result] = {
    for {
      extendedCollectible <- collectibleService.getCollectible(collectibleId)
      typeData <- modelCollectibleService.getCompleteType(extendedCollectible.collectible.typeId)
    } yield {
      val (entityType, constraints) = typeData
      val editForm = if (form.isDefined) form.get else EntityForm.form.fill(
        EntityForm.Data(
          extendedCollectible.properties.map(_.value), Seq(), Seq(), Seq()))

      Ok(views.html.container.subject.collectible_editor(entityType,
        extendedCollectible,
        collectibleService.getCollectiblePropertyKeys(constraints),
        collectibleService.getObligatoryPropertyKeys(constraints),
        editForm, msg, successMsg))
    }
  } recoverWith {
    case e =>
      logger.error(e.getMessage, e)
      Future.successful(Redirect(routes.CollectionController.getCollection(collectionId)).flashing("error" -> e.getMessage))
  }

}


