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
import modules.core.model.{Constraint, EntityType}
import modules.core.service.{EntityTypeService, ModelEntityService}
import modules.subject.model.CollectionConstraintSpec
import modules.subject.service.ModelCollectionService
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
                                entityTypeService: EntityTypeService)
  extends AbstractController(cc) with I18nSupport with Logging with Authentication {

  private def getService(typeId: Long)(implicit ticket: Ticket): Future[ModelEntityService] = {
    entityTypeService.getType(typeId) map {
      case t if t.isEmpty => throw new Exception("No such EntityType found")
      case t if t.get.typeOf == AssetConstraintSpec.ASSET => modelAssetService
      case t if t.get.typeOf == CollectionConstraintSpec.COLLECTION => modelCollectionService
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
          //FIXME thats just a model overview, remove asset naming
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
  def addType: Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        NewTypeForm.form.bindFromRequest fold(
          errorForm => {
            Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> "Invalid form data!"))
          },
          data => {
            entityTypeService.addType(data.value, data.typeOf) map { _ =>
              Redirect(routes.ModelController.index())
            } recoverWith {
              case e => {
                logger.error(e.getMessage, e)
                Future.successful(Redirect(routes.ModelController.index()).flashing("error" -> e.getMessage))
              }
            }
          })
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
   * @param id of the EntityType which shall be edited
   * @return model overview page with open editor and optional error message
   *
   */
  //FIXME the problem is, that the editor must also always render the whole left side types.
  //FIXME unless this is somehow changed or outsourced, the weired form param flashes wont't go away..
  def getTypeEditor(id: Long):
  Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket {
        implicit ticket =>
          entityTypeService.getCombinedEntity(id) map (res =>
            ((allTypes: Seq[EntityType], editedType: Option[EntityType], constraints: Seq[Constraint]) => {
              if (editedType.nonEmpty) {
                //FIXME this is really strange code...
                val preparedEditForm = EditTypeForm.form.fill(EditTypeForm.Data(editedType.get.value, editedType.get.active))
                var preparedConstraintForm = NewConstraintForm.form.fill(NewConstraintForm.Data("", "", ""))
                val c = request.flash.get("c")
                val v1 = request.flash.get("v1")
                val v2 = request.flash.get("v2")
                if (c.isDefined && v1.isDefined && v2.isDefined) {
                  preparedConstraintForm = NewConstraintForm.form.fill(NewConstraintForm.Data(c.get, v1.get, v2.get))
                }
                val error = request.flash.get("error")
                Ok(views.html.container.core.model_editor(allTypes, editedType.get, constraints, preparedEditForm, preparedConstraintForm, error))
              } else {
                Redirect(routes.ModelController.index()).flashing("error" -> "Entity Type not found")
              }
            }).tupled(res)) recoverWith {
            case e: Throwable => {
              logger.error(e.getMessage, e)
              Future.successful(Redirect(routes.ModelController.getTypeEditor(id)).flashing(("error" -> e.getMessage)))
            }
          }
      }
  }

  //TODO
  def searchEntityType(): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket {
        implicit ticket =>
          //FIXME
          Future.successful(Redirect(routes.ModelController.getTypeEditor(0)))
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
  def addConstraint(typeId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket {
        implicit ticket =>
          NewConstraintForm.form.bindFromRequest fold(
            errorForm => {
              //ignore form input here, just show an error message, maybe a future FIXME
              Future.successful(Redirect(routes.ModelController.getTypeEditor(typeId)).flashing("error" -> "Invalid form data!"))
            },
            data => {
              getService(typeId) flatMap (_.addConstraint(data.c, data.v1, data.v2, typeId) map { id =>
                Redirect(routes.ModelController.getTypeEditor(typeId))
              })
            } recoverWith {
              case e => {
                logger.error(e.getMessage, e)
                //This should be done more elegantly... FIXME
                Future.successful(Redirect(routes.ModelController.getTypeEditor(typeId)).flashing(
                  "error" -> e.getMessage, "c" -> data.c, "v1" -> data.v1, "v2" -> data.v2))
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
  def deleteConstraint(typeId: Long, constraintId: Long): Action[AnyContent] = withAuthentication.async {
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
