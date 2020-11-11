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

import formdata.asset.{EditAssetTypeForm, NewAssetConstraintForm, NewAssetTypeForm}
import javax.inject.{Inject, Singleton}
import middleware.{AuthenticatedRequest, Authentication, AuthenticationFilter}
import model.asset.AssetType
import model.generic.Constraint
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.asset.ModelAssetService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The ModelAssetController responsible for all endpoints regarding asset type creation and management
 *
 * @param cc                 injected ControllerComponents
 * @param withAuthentication injected AuthenticationAction
 * @param modelService       injected ModelService for business logic
 */
@Singleton
class ModelAssetController @Inject()(cc: ControllerComponents, withAuthentication: AuthenticationFilter, modelService: ModelAssetService)
  extends AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Endpoint to show the model overview page
   *
   * @return model overview page
   */
  def index: Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        modelService.getAllAssetTypes map (types => {
          val error = request.flash.get("error")
          Ok(views.html.container.asset.model_asset_overview(types, error))
        }) recoverWith {
          case e => {
            logger.error(e.getMessage, e)
            Future.successful(Ok(views.html.container.asset.model_asset_overview(Seq(), Option(e.getMessage))))
          }
        }
      }
  }

  /**
   * Endpoint to add a new asset type to the model.
   * Only the Modeler role is able to perform this action.
   *
   * @return model overview page with optional error message
   */
  def addAssetType: Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        NewAssetTypeForm.form.bindFromRequest fold(
          errorForm => {
            Future.successful(Redirect(routes.ModelAssetController.index()).flashing("error" -> "Invalid form data!"))
          },
          data => {
            val assetType = AssetType(0, data.value, active = false);
            modelService.addAssetType(assetType) map { _ =>
              Redirect(routes.ModelAssetController.index())
            } recoverWith {
              case e => {
                logger.error(e.getMessage, e)
                Future.successful(Redirect(routes.ModelAssetController.index()).flashing("error" -> e.getMessage))
              }
            }
          })
      }
  }

  /**
   * Endpoint to delete an asset type from the model.
   * Only the Modeler role is able to perform this action.
   *
   * @param id id of the asset type to delete
   * @return model overview page with optional error message
   */
  def deleteAssetType(id: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        modelService.deleteAssetType(id) map {
          _ => Redirect(routes.ModelAssetController.index())
        } recoverWith {
          case e => {
            logger.error(e.getMessage, e)
            Future.successful(Redirect(routes.ModelAssetController.index()).flashing("error" -> e.getMessage))
          }
        }
      }
  }

  /**
   * Endpoint to get the model overview with open asset editor.
   * Only the Modeler role is able to perform this action.
   *
   * @param id  of the asset type which shall be edited
   * @return model overview page with open editor and optional error message
   *
   */
    //FIXME the problem is, that the editor must also always render the whole left side types.
    //FIXME unless this is somehow changed or outsourced, the weired form param flashes wont't go away..
  def getAssetTypeEditor(id: Long):
  Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      modelService.getCombinedAssetEntity(id) map (res =>
        ((assetTypes: Seq[AssetType], assetType: Option[AssetType], constraints: Seq[Constraint]) => {
          if (assetType.nonEmpty) {
            val preparedAssetForm = EditAssetTypeForm.form.fill(EditAssetTypeForm.Data(assetType.get.value, assetType.get.active))
            var preparedConstraintForm = NewAssetConstraintForm.form.fill(NewAssetConstraintForm.Data("", "", ""))
            val c = request.flash.get("c")
            val v1 = request.flash.get("v1")
            val v2 = request.flash.get("v2")
            if (c.isDefined && v1.isDefined && v2.isDefined) {
              preparedConstraintForm = NewAssetConstraintForm.form.fill(NewAssetConstraintForm.Data(c.get, v1.get, v2.get))
            }
            val error = request.flash.get("error")
            Ok(views.html.container.asset.model_asset_editor(assetTypes, assetType.get, constraints, preparedAssetForm, preparedConstraintForm, error))
          } else {
            Redirect(routes.ModelAssetController.index()).flashing("error" -> "Asset Type not found")
          }
        }).tupled(res)) recoverWith {
        case e: Throwable => {
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.ModelAssetController.getAssetTypeEditor(id)).flashing(("error" -> e.getMessage)))
        }
      }
    }
  }

  //TODO
  def searchAssetType(): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        //FIXME
        Future.successful(Redirect(routes.ModelAssetController.getAssetTypeEditor(0)))
      }
  }

  /**
   * Endpoint to change an asset type (update activation or value).
   * Only the Modeler role is able to perform this action.
   *
   * @param id of the asset type to change (is not transmitted by the form)
   * @return model page with open asset type editor
   */
  def postAssetType(id: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        EditAssetTypeForm.form.bindFromRequest fold(
          errorForm => {
            //ignore form input here, just show an error message, maybe a future FIXME
            Future.successful(Redirect(routes.ModelAssetController.getAssetTypeEditor(id)).flashing("error" -> "Invalid form data!"))
          },
          data => {
            val assetType = AssetType(id, data.value, data.active)
            modelService.updateAssetType(assetType) map { _ =>
              Redirect(routes.ModelAssetController.getAssetTypeEditor(id))
            } recoverWith {
              case e => {
                logger.error(e.getMessage, e)
                Future.successful(Redirect(routes.ModelAssetController.getAssetTypeEditor(id)).flashing("error" -> e.getMessage))
              }
            }
          })
      }
  }

  /**
   * Endpoint to add a new AssetConstraint to the specified AssetType
   *
   * @param assetTypeId id of the parent AssetType
   * @return redirects to getAssetTypeEditor with optional form presets and error message
   */
  def addAssetConstraint(assetTypeId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        NewAssetConstraintForm.form.bindFromRequest fold(
          errorForm => {
            //ignore form input here, just show an error message, maybe a future FIXME
            Future.successful(Redirect(routes.ModelAssetController.getAssetTypeEditor(assetTypeId)).flashing("error" -> "Invalid form data!"))
          },
          data => {
            val assetConstraint = Constraint(0, data.c, data.v1, data.v2, assetTypeId)
            modelService.addConstraint(assetConstraint) map { id =>
              Redirect(routes.ModelAssetController.getAssetTypeEditor(assetTypeId))
            } recoverWith {
              case e => {
                logger.error(e.getMessage, e)
                //This should be done more elegantly... FIXME
                Future.successful(Redirect(routes.ModelAssetController.getAssetTypeEditor(assetTypeId)).flashing(
                  "error" -> e.getMessage, "c" -> data.c, "v1" -> data.v1, "v2" -> data.v2))
              }
            }
          })
      }
  }

  /**
   * Endpoint to delete an AssetConstraint
   *
   * @param assetTypeId  id of the parent AssetType
   * @param constraintId id of the AssetConstraint to delete
   * @return redirects to getAssetTypeEditor with optional error message
   */
  def deleteAssetConstraint(assetTypeId: Long, constraintId: Long): Action[AnyContent] = withAuthentication.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        modelService.deleteConstraint(constraintId) map {
          _ => Redirect(routes.ModelAssetController.getAssetTypeEditor(assetTypeId))
        } recoverWith {
          case e => {
            logger.error(e.getMessage, e)
            Future.successful(Redirect(routes.ModelAssetController.getAssetTypeEditor(assetTypeId)).flashing(("error" -> e.getMessage)))
          }
        }
      }
  }

}
