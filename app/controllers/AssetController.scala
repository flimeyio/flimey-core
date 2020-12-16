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

import asset.formdata.{NewAssetForm, SelectAssetTypeForm}
import asset.service.AssetService
import assetmodel.service.ModelAssetService
import auth.model.Ticket
import javax.inject.{Inject, Singleton}
import middleware.{AuthenticatedRequest, Authentication, AuthenticationFilter}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The AssetController responsible for all endpoints regarding asset creation and management.
 *
 * @param cc                 injected ControllerComponents
 * @param assetService       injected AssetService for business logic
 * @param modelAssetService  injected ModelAssetService for business logic
 * @param withAuthentication injected AuthenticationFilter
 */
@Singleton
class AssetController @Inject()(cc: ControllerComponents, withAuthentication: AuthenticationFilter,
                                assetService: AssetService, modelAssetService: ModelAssetService) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Endpoint to show the asset overview page.<br />
   * No AssetType is initially selected.
   *
   * @return asset overview page
   */
  def index: Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        modelAssetService.getAllAssetTypes map (types => {
          val error = request.flash.get("error")
          Ok(views.html.container.asset.asset_overview(None, types, Seq(), error))
        }) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Ok(views.html.container.asset.asset_overview(None, Seq(), Seq(), Option(e.getMessage))))
        }
      }
    }

  def searchAssets: Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        modelAssetService.getAllAssetTypes map (types => {
          //FIXME
          Ok(views.html.container.asset.asset_overview(None, types, Seq(), None))
        })
      }
    }

  /**
   * Endpoint to change the shown AssetType in the selection<br />
   * Resets the search query an other filters.<br />
   * Does not generate a result but redirects to getAssetOfType() with updated AssetType index parameter.
   *
   * @return asset overview page
   */
  def changeAssetType: Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        SelectAssetTypeForm.form.bindFromRequest fold(
          errorForm => {
            Future.successful(Redirect(routes.AssetController.index()).flashing("error" -> "No such Asset Type found"))
          },
          data => {
            val assetTypeValue = data.value
            modelAssetService.getAssetTypeByValue(assetTypeValue) flatMap (assetType => {
              if (assetType.isEmpty) Future.failed(new Exception("No such AssetType found"))
              if (assetType.isDefined) {
                Future.successful(Redirect(routes.AssetController.getAssetsOfType(assetType.get.id)))
              } else {
                Future.failed(new Exception("No Asset Type selected"))
              }
            }) recoverWith {
              case e =>
                logger.error(e.getMessage, e)
                Future.successful(Redirect(routes.AssetController.index()).flashing("error" -> e.getMessage))
            }
          })
      }
    }

  /**
   * Endpoint to show Assets of a selected AssetType.<br />
   * <br />
   * Not implemented yet: show only a range<br />
   * Not implemented yet: accept search parameter<br />
   *
   * @param assetTypeId id of the shown AssetType
   * @return asset overview with (currently) all Assets of the selected type
   */
  def getAssetsOfType(assetTypeId: Long):
  Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      assetService.getAssetComplex(assetTypeId) map (assetComplex => {
        //FIXME actually fetch the assets or some part of them (additional url query params)
        val error = request.flash.get("error")
        Ok(views.html.container.asset.asset_overview(assetComplex.parentAssetType, assetComplex.allAssetTypes, assetComplex.children, error))
      }) recoverWith {
        case e =>
          logger.error(e.getMessage, e)
          Future.successful(Redirect(routes.AssetController.index()).flashing("error" -> e.getMessage))
      }
    }
  }

  /**
   * Endpoint to get an editor to create new Assets.<br />
   * The Editor will only accept Assets of the previously selected (currently active) AssetType.
   *
   * @param assetTypeId id of the AssetType
   * @return new asset editor
   */
  def getNewAssetEditor(assetTypeId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        val newAssetForm = NewAssetForm.form.fill(NewAssetForm.Data(Seq()))
        val error = request.flash.get("error")
        newAssetEditorFactory(assetTypeId, newAssetForm, error)
      }
    }

  /**
   * Endpoint to add a new Asset.<br />
   * The Asset must be of the selected AssetType.<br />
   * The incoming form data seq must be in the same order as the previously sent property keys.
   *
   * @param assetTypeId id of the AssetType
   * @return new asset editor (clean or with errors)
   */
  def addNewAsset(assetTypeId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        NewAssetForm.form.bindFromRequest fold(
          errorForm => newAssetEditorFactory(assetTypeId, errorForm, Option("Error validation form data")),
          data => {
            assetService.addAsset(assetTypeId, data.values) flatMap (_ => {
              val newAssetForm = NewAssetForm.form.fill(NewAssetForm.Data(Seq()))
              newAssetEditorFactory(assetTypeId, newAssetForm, None, Option("Asset successfully created"))
            }) recoverWith {
              case e =>
                logger.error(e.getMessage, e)
                val newAssetForm = NewAssetForm.form.fill(data)
                newAssetEditorFactory(assetTypeId, newAssetForm, Option(e.getMessage))
            }
          })
      }
    }

  /**
   * Endpoint to get the current asset overview with opened asset editor.<br />
   * The Asset to edit must part of the current overview selection.
   *
   * @param assetTypeId id of the AssetType
   * @param assetId     id of the Asset to edit
   * @return editor view result future
   */
  def getAssetEditor(assetTypeId: Long, assetId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        val error = request.flash.get("error")
        assetEditorFactory(assetTypeId, assetId, None, error)
      }
    }

  /**
   * Endpoint to post (update) the data of the currently edited Asset.<br />
   * The Asset must be part of the current overview selection.
   *
   * @param assetTypeId id of the AssetType
   * @param assetId     id of the Asset to edit
   * @param msg         optional error message
   * @return editor view result future
   */
  def postAsset(assetTypeId: Long, assetId: Long, msg: Option[String] = None): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        NewAssetForm.form.bindFromRequest fold(
          errorForm => assetEditorFactory(assetTypeId, assetId, Option(errorForm), Option("Error validation form data")),
          data => {
            assetService.updateAssetProperties(assetId, data.values) flatMap (_ => {
              assetEditorFactory(assetTypeId, assetId, Option(NewAssetForm.form.fill(data)), None)
            }) recoverWith {
              case e =>
                logger.error(e.getMessage, e)
                val newAssetForm = NewAssetForm.form.fill(data)
                assetEditorFactory(assetTypeId, assetId, Option(newAssetForm), Option(e.getMessage))
            }
          })
      }
    }

  /**
   * Endpoint to delete an Asset.<br />
   * The Asset is deleted permanently and can not be restored!
   *
   * @param assetTypeId id of the parent AssetType
   * @param assetId     id of the Asset to delete
   * @param msg         optional error message
   * @return
   */
  def deleteAsset(assetTypeId: Long, assetId: Long, msg: Option[String] = None): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        assetService.deleteAsset(assetId) map (_ =>
          Redirect(routes.AssetController.getAssetsOfType(assetTypeId))
          ) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Redirect(routes.AssetController.getAssetEditor(assetTypeId, assetId)).flashing("error" -> e.getMessage))
        }
      }
    }

  /**
   * Helper function to build a 'new asset editor' view based on different configuration parameters.
   *
   * @param assetTypeId id of the AssetType
   * @param form        NewAssetForm, which can be already filled
   * @param errmsg      optional error message
   * @param succmsg     optional positive message
   * @param request     implicit request context
   * @return new asset editor result future (view)
   */
  private def newAssetEditorFactory(assetTypeId: Long, form: Form[NewAssetForm.Data], errmsg: Option[String] = None, succmsg: Option[String] = None)
                                   (implicit request: Request[AnyContent], ticket: Ticket): Future[Result] = {
    modelAssetService.getCompleteAssetType(assetTypeId) map (typeData => {
      val (assetType, constraints) = typeData
      if (assetType.isDefined) {
        Ok(views.html.container.asset.new_asset_editor(assetType.get,
          assetService.getAssetPropertyKeys(constraints),
          assetService.getObligatoryPropertyKeys(constraints),
          form, errmsg, succmsg))
      } else {
        Redirect(routes.AssetController.index()).flashing("error" -> "No such Asset Type found or editable")
      }
    }) recoverWith {
      case e =>
        logger.error(e.getMessage, e)
        Future.successful(Redirect(routes.AssetController.index()).flashing("error" -> e.getMessage))
    }
  }

  /**
   * Helper function to build a 'asset editor' view based on different configuration parameters.
   *
   * @param assetTypeId id of the AssetType
   * @param assetId     id of the Asset to edit
   * @param form        optional prepared form data
   * @param msg         optional error message
   * @param request     implicit request context
   * @return asset editor result future (view)
   */
  private def assetEditorFactory(assetTypeId: Long, assetId: Long, form: Option[Form[NewAssetForm.Data]], msg: Option[String] = None)
                                (implicit request: Request[AnyContent], ticket: Ticket): Future[Result] = {
    assetService.getAssetComplex(assetTypeId) flatMap (assetComplex => {
      val editedAssetData = assetComplex.children.find(_._1.id == assetId)
      if (editedAssetData.isEmpty) throw new Exception("Edited Asset is not part of current selection")
      if (assetComplex.parentAssetType.isDefined) {
        modelAssetService.getCompleteAssetType(assetComplex.parentAssetType.get.id) map (typeData => {
          val (_, constraints) = typeData
          val editForm = if (form.isDefined) form.get else NewAssetForm.form.fill(NewAssetForm.Data(editedAssetData.get._2.map(_.value)))
          Ok(views.html.container.asset.asset_overview_with_editor(assetComplex.parentAssetType.get,
            assetComplex.allAssetTypes,
            assetComplex.children,
            assetId,
            assetService.getAssetPropertyKeys(constraints),
            assetService.getObligatoryPropertyKeys(constraints),
            editForm, msg))
        })
      } else {
        Future.successful(Redirect(routes.AssetController.index()).flashing("error" -> "No such Asset Type"))
      }
    }) recoverWith {
      case e =>
        logger.error(e.getMessage, e)
        Future.successful(Redirect(routes.AssetController.getAssetsOfType(assetTypeId)).flashing("error" -> e.getMessage))
    }
  }

}


