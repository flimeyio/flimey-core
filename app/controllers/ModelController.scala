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
 * @param cc                      injected ControllerComponents
 * @param withAuthentication      injected AuthenticationAction
 * @param modelAssetService       injected [[modules.asset.service.ModelAssetService ModelAssetService]] for Asset model logic
 * @param modelCollectionService  injected [[modules.subject.service.ModelCollectionService ModelCollectionService]] for Collection model logic
 * @param modelCollectibleService injected [[modules.subject.service.ModelCollectibleService MidelCollectibleService]] for Collectible model logic
 * @param entityTypeService       injected [[modules.core.service.EntityTypeService EntityTypeService]] for generic type logic
 */
@Singleton
class ModelController @Inject()(cc: ControllerComponents,
                                withAuthentication: AuthenticationFilter,
                                modelAssetService: ModelAssetService,
                                modelCollectionService: ModelCollectionService,
                                modelCollectibleService: ModelCollectibleService,
                                entityTypeService: EntityTypeService)
  extends AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Private hub method to decide on the used ModelService strategy based on the type of the given id.
   * <p> This works because all ModelServices implement the [[modules.core.service.ModelEntityService]] ModelEntityService trait
   * and all methods inside this controller only use the interface defined by this trait.
   * <p> Depending on the given type, the returned ModelService will provide type specific service functionality.
   *
   * @param typeId id of the [[modules.core.model.EntityType EntityType]]
   * @param ticket implicit authentication ticket
   * @return Future[ModelEntityService] (type specific implementation)
   */
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
        entityTypeService.getAllVersions() map (types => {
          val error = request.flash.get("error")
          val groupedTypes = types.groupBy(_.entityType.id).mapValues(
            _.sortBy(_.version.version)).values.toSeq.sortBy(_.head.entityType.id)
          Ok(views.html.container.core.model_overview(groupedTypes, error))
        }) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Ok(views.html.container.core.model_overview(Seq(), Option(e.getMessage))))
        }
      }
  }

  /**
   * Endpoint to add a new [[modules.core.model.EntityType EntityType]] to the model.
   *
   * @return model overview page with optional error message
   */
  def addType(): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        NewTypeForm.form.bindFromRequest fold(
          _ => {
            Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> "Invalid form data!"))
          },
          data => {
            val parentName = data.typeOf
            if (!Seq(AssetConstraintSpec.ASSET, CollectionConstraintSpec.COLLECTION, CollectibleConstraintSpec.COLLECTIBLE).contains(parentName)) {
              Future.failed(new Exception("Select a valid parent type!"))
            } else {
              entityTypeService.addType(data.value, parentName) map { _ =>
                Redirect(routes.ModelController.index())
              }
            }
          }) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> e.getMessage))
        }
      }
  }

  /**
   * Endpoint to add a new [[modules.core.model.TypeVersion TypeVersion]] of a specified
   * [[modules.core.model.EntityType EntityType]].
   *
   * @param typeId id of the parent EntityType
   * @return redirect to ModelController.index()
   */
  def addVersion(typeId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        getService(typeId) flatMap (_.addVersion(typeId) map (_ => Redirect(routes.ModelController.index())))
      } recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> e.getMessage))
      }
  }

  /**
   * Endpoint to fork (clone) a already existing [[modules.core.model.TypeVersion TypeVersion]] of a specified
   * [[modules.core.model.EntityType EntityType]].
   * <p> This will create a new TypeVersion with the same data but a higher version number.
   *
   * @see [[modules.core.service.ModelEntityService#forkVersion]]
   * @param typeId    id of the parent EntityType
   * @param versionId id of the TypeVersion to fork
   * @return redirect to ModelController.index()
   */
  def forkVersion(typeId: Long, versionId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        getService(typeId) flatMap (_.forkVersion(versionId) map (_ => Redirect(routes.ModelController.index())))
      } recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> e.getMessage))
      }
  }

  /**
   * Endpoint to delete a [[modules.core.model.TypeVersion TypeVersion]].
   * <p> <strong>This is a very destructive operation!</strong>
   *
   * @see [[modules.core.service.ModelEntityService#deleteVersion]]
   * @param typeId    id of the parent [[modules.core.model.EntityType EntityType]]
   * @param versionId id of the TypeVersion to delete
   * @return redirect to ModelController.index()
   */
  def deleteVersion(typeId: Long, versionId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        getService(typeId) flatMap (_.deleteVersion(versionId) map (_ => Redirect(routes.ModelController.index())))
      } recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> e.getMessage))
      }
  }

  /**
   * Endpoint to delete an [[modules.core.model.EntityType EntityType]] from the model.
   * <p> Further actions depend on the specific EntityType
   * <p> <strong>This is a very destructive operation!</strong>
   *
   * @see [[modules.core.service.ModelEntityService#deleteType]]
   * @param id id of the EntityType to delete
   * @return model overview page with optional error message
   */
  def deleteType(id: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        getService(id) flatMap (_.deleteType(id) map (_ => Redirect(routes.ModelController.index())))
      } recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> e.getMessage))
      }
  }

  /**
   * Endpoint to get the open [[modules.core.model.EntityType EntityType]] editor.
   *
   * @param typeId of the EntityType which shall be edited
   * @return model overview page with open editor and optional error message
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
        case e: Throwable =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to get the open [[modules.core.model.Constraint Constraint]] editor of a specified
   * [[modules.core.model.TypeVersion TypeVersion]].
   *
   * @param typeId    of the [[modules.core.model.EntityType EntityType]]
   * @param versionId of the TypeVersion to edit
   * @return model overview page with open editor and optional error message
   */
  def getVersionEditor(typeId: Long, versionId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket {
        implicit ticket =>
          entityTypeService.getExtendedType(versionId) map (extendedEntityType => {
            val preparedConstraintForm = NewConstraintForm.form.fill(NewConstraintForm.Data("", "", ""))
            val error = request.flash.get("error")
            Ok(views.html.container.core.model_version_editor(extendedEntityType, preparedConstraintForm, error))
          }) recoverWith {
            case e: Throwable =>
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> e.getMessage))
          }
      }
  }

  /**
   * Endpoint to change an [[modules.core.model.EntityType EntityType]] (update activation or value).
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
            _ => {
              //ignore form input here, just show an error message
              Future.successful(Redirect(routes.ModelController.getTypeEditor(id)).flashing("error" -> "Invalid form data!"))
            },
            data => {
              getService(id) flatMap (_.updateType(id, data.value, data.active) map { _ =>
                Redirect(routes.ModelController.getTypeEditor(id))
              })
            }) recoverWith {
            case e =>
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.ModelController.getTypeEditor(id)).flashing("error" -> e.getMessage))
          }
      }
  }

  /**
   * Endpoint to add a new [[modules.core.model.Constraint Constraint]] to the specified [[modules.core.model.TypeVersion TypeVersion]]
   * <p> Further actions depend on the specific EntityType
   *
   * @param typeId    id of the parent [[modules.core.model.EntityType EntityType]]
   * @param versionId id of the TypeVersion to edit
   * @return redirects to constraint editor with optional form presets and error message
   */
  def addConstraint(typeId: Long, versionId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket {
        implicit ticket =>
          NewConstraintForm.form.bindFromRequest fold(
            _ => {
              Future.successful(Redirect(routes.ModelController.getVersionEditor(typeId, versionId)).flashing("error" -> "Invalid form data!"))
            },
            data => {
              getService(typeId) flatMap (_.addConstraint(data.c, data.v1, data.v2, versionId) map { _ =>
                Redirect(routes.ModelController.getVersionEditor(typeId, versionId))
              })
            } recoverWith {
              case e =>
                logger.error(e.getMessage, e)
                Future.successful(Redirect(routes.ModelController.getVersionEditor(typeId, versionId)).flashing("error" -> e.getMessage))
            })
      }
  }

  /**
   * Endpoint to delete a [[modules.core.model.Constraint Constraint]] of the specified [[modules.core.model.TypeVersion TypeVersion]]
   * <p> <strong>This is a very destructive operation!</strong>
   *
   * @see [[modules.core.service.ModelEntityService#deleteConstraint]]
   * @param typeId       id of the parent [[modules.core.model.EntityType EntityType]]
   * @param versionId    id of the TypeVersion to edit
   * @param constraintId id of the Constraint to delete
   * @return redirects to getTypeEditor with optional error message
   */
  def deleteConstraint(typeId: Long, versionId: Long, constraintId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket {
        implicit ticket =>
          getService(typeId) flatMap (_.deleteConstraint(constraintId) map { _ =>
            Redirect(routes.ModelController.getVersionEditor(typeId, versionId))
          }) recoverWith {
            case e =>
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.ModelController.getVersionEditor(typeId, versionId)).flashing("error" -> e.getMessage))
          }
      }
  }

}