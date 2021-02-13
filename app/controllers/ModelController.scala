/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020-2021  Karl Kegel
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
import modules.asset.model.AssetConstraintSpec
import modules.asset.service.ModelAssetService
import modules.auth.model.Ticket
import modules.core.formdata.{EditTypeForm, NewConstraintForm, NewTypeForm}
import modules.core.service.{EntityTypeService, ModelEntityService}
import modules.subject.model.{CollectibleConstraintSpec, CollectionConstraintSpec}
import modules.subject.service.{ModelCollectibleService, ModelCollectionService}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The ModelAssetController responsible for all endpoints regarding asset type creation and management
 *
 * @param cc                 injected ControllerComponents
 * @param withAuthentication injected AuthenticationAction
 * @param modelAssetService  injected ModelService for business logic
 */
@Singleton
class ModelController @Inject()(cc: ControllerComponents,
                                withAuthentication: AuthenticationFilter,
                                modelAssetService: ModelAssetService,
                                modelCollectionService: ModelCollectionService,
                                modelCollectibleService: ModelCollectibleService,
                                entityTypeService: EntityTypeService)
  extends AbstractController(cc) with I18nSupport with Logging with Authentication {

  private def getService(typeId: Long)(implicit ticket: Ticket): Future[ModelEntityService] = {
    entityTypeService.getType(typeId) map {
      case t if t.isEmpty => throw new Exception("No such EntityType found")
      case t if t.get.typeOf == AssetConstraintSpec.ASSET => modelAssetService
      case t if t.get.typeOf == CollectionConstraintSpec.COLLECTION => modelCollectionService
      case t if t.get.typeOf == CollectibleConstraintSpec.COLLECTIBLE => modelCollectibleService
      case _ => throw new Exception("Unknown error fetching EntityType")
    }
  }

  /**
   * Endpoint to show the model overview page
   *
   * @return model overview page
   */
  def index: Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        entityTypeService.getAllTypes() map (types => {
          val error = request.flash.get("error")
          Ok(views.html.container.core.model_overview(types, error))
        }) recoverWith {
          case e => {
            logger.error(e.getMessage, e)
            Future.successful(Ok(views.html.container.core.model_overview(Seq(), Option(e.getMessage))))
          }
        }
      }
  }

  /**
   * Endpoint to add a new EntityType to the model.
   *
   * @return model overview page with optional error message
   */
  def addType(): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        NewTypeForm.form.bindFromRequest fold(
          errorForm => {
            Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> "Invalid form data!"))
          },
          data => {
            val parentName = data.typeOf;
            if (!Seq(AssetConstraintSpec.ASSET, CollectionConstraintSpec.COLLECTION, CollectibleConstraintSpec.COLLECTIBLE).contains(parentName)) {
              Future.failed(new Exception("Select a valid parent type!"))
            } else {
              entityTypeService.addType(data.value, parentName) map { _ =>
                Redirect(routes.ModelController.index())
              }
            }
          }) recoverWith {
          case e => {
            logger.error(e.getMessage, e)
            Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> e.getMessage))
          }
        }
      }
  }

  def addVersion(typeId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> "Not implemented yet"))
      }
  }

  def deleteVersion(typeId: Long, versionId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> "Not implemented yet"))
      }
  }

  def forkVersion(typeId: Long, versionId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> "Not implemented yet"))
      }
  }

  /**
   * Endpoint to delete an EntityType from the model.
   * <p> Further actions depend on the specific EntityType
   *
   * @param id id of the type to delete
   * @return model overview page with optional error message
   */
  def deleteType(id: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        getService(id) flatMap (_.deleteType(id) map (_ => Redirect(routes.ModelController.index())))
      } recoverWith {
        case e => {
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> e.getMessage))
        }
      }
  }

  /**
   * Endpoint to get the model overview with open constraint editor.
   *
   * @param typeId of the EntityType which shall be edited
   * @return model overview page with open editor and optional error message
   *
   */
  def getTypeEditor(typeId: Long): Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      entityTypeService.getType(typeId) map (editedType =>
        if (editedType.nonEmpty) {
          val preparedEditForm = EditTypeForm.form.fill(EditTypeForm.Data(editedType.get.value, editedType.get.active))
          val error = request.flash.get("error")
          Ok(views.html.container.core.model_type_editor(editedType.get, preparedEditForm, error))
        } else {
          Redirect(routes.ModelController.index()).flashing("error" -> "Entity Type not found")
        }) recoverWith {
        case e: Throwable => {
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ModelController.index()).flashing(("error" -> e.getMessage)))
        }
      }
    }
  }

  def getVersionEditor(typeId: Long, versionId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket {
        implicit ticket =>
          entityTypeService.getExtendedType(versionId) map (extendedEntityType => {
            var preparedConstraintForm = NewConstraintForm.form.fill(NewConstraintForm.Data("", "", ""))
            val error = request.flash.get("error")
            Ok(views.html.container.core.model_version_editor(extendedEntityType, preparedConstraintForm, error))
          }) recoverWith {
            case e: Throwable => {
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.ModelController.index()).flashing(("error" -> e.getMessage)))
            }
          }
      }
  }

  /**
   * Endpoint to change an EntityType (update activation or value).
   * <p> Further actions depend on the specific EntityType
   *
   * @param id of the EntityType to change (is not transmitted by the form)
   * @return model page with open asset type editor
   */
  def postEntityType(id: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket {
        implicit ticket =>
          EditTypeForm.form.bindFromRequest fold(
            errorForm => {
              //ignore form input here, just show an error message, maybe a future FIXME
              Future.successful(Redirect(routes.ModelController.getTypeEditor(id)).flashing("error" -> "Invalid form data!"))
            },
            data => {
              getService(id) flatMap (_.updateType(id, data.value, data.active) map { _ =>
                Redirect(routes.ModelController.getTypeEditor(id))
              })
            }) recoverWith {
            case e => {
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.ModelController.getTypeEditor(id)).flashing("error" -> e.getMessage))
            }
          }
      }
  }

  /**
   * Endpoint to add a new AssetConstraint to the specified AssetType
   * <p> Further actions depend on the specific EntityType
   *
   * @param typeId id of the parent EntityType
   * @return redirects to type editor with optional form presets and error message
   */
  def addConstraint(typeId: Long, versionId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket {
        implicit ticket =>
          NewConstraintForm.form.bindFromRequest fold(
            errorForm => {
              Future.successful(Redirect(routes.ModelController.getTypeEditor(typeId)).flashing("error" -> "Invalid form data!"))
            },
            data => {
              getService(typeId) flatMap (_.addConstraint(data.c, data.v1, data.v2, typeId) map { id =>
                Redirect(routes.ModelController.getTypeEditor(typeId))
              })
            } recoverWith {
              case e => {
                logger.error(e.getMessage, e)
                Future.successful(Redirect(routes.ModelController.getTypeEditor(typeId)).flashing("error" -> e.getMessage))
              }
            })
      }
  }

  /**
   * Endpoint to delete an Constraint
   *
   * @param typeId       id of the parent EntityType
   * @param constraintId id of the Constraint to delete
   * @return redirects to getTypeEditor with optional error message
   */
  def deleteConstraint(typeId: Long, versionId: Long, constraintId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket {
        implicit ticket =>
          getService(typeId) flatMap (_.deleteConstraint(constraintId) map { _ =>
            Redirect(routes.ModelController.getTypeEditor(typeId))
          }) recoverWith {
            case e => {
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.ModelController.getTypeEditor(typeId)).flashing(("error" -> e.getMessage)))
            }
          }
      }
  }

}
