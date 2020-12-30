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
import asset.service.{AssetService, ModelAssetService}
import auth.model.Ticket
import javax.inject.{Inject, Singleton}
import middleware.{AuthenticatedRequest, Authentication, AuthenticationFilter}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import user.service.GroupService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The AssetController responsible for all endpoints regarding asset creation and management.
 *
 * @param cc                 injected ControllerComponents
 * @param assetService       injected AssetService for business logic
 * @param modelAssetService  injected ModelAssetService for business logic
 * @param withAuthentication injected AuthenticationFilter
 * @param groupService       injected GroupService
 */
@Singleton
class AssetController @Inject()(cc: ControllerComponents, withAuthentication: AuthenticationFilter,
                                assetService: AssetService, modelAssetService: ModelAssetService, groupService: GroupService) extends
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
          Ok(views.html.container.asset.asset_overview(None, types, Seq(), 0, error))
        }) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Ok(views.html.container.asset.asset_overview(None, Seq(), Seq(), 0, Option(e.getMessage))))
        }
      }
    }

  def searchAssets: Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        modelAssetService.getAllAssetTypes map (types => {
          //FIXME
          Ok(views.html.container.asset.asset_overview(None, types, Seq(), 0, None))
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
                Future.successful(Redirect(routes.AssetController.getAssets(assetType.get.id, 0)))
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
   * Endpoint to get a number of Assets based on multiple query parameters.<br />
   * In every case, only Assets which can be accessed based on the Ticket can be selected.<br />
   *
   * @param assetTypeId   id of the AssetType the Assets must have
   * @param pageNumber    number of the selection window - see AssetService.getAsset()
   * @param groupSelector string containing group ids which the Assets can have (filter) in form "id,id,..."
   * @return
   */
  def getAssets(assetTypeId: Long, pageNumber: Int, groupSelector: Option[String] = None):
  Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      //FIXME set pageSize to something around 50
      assetService.getAssetComplex(assetTypeId, pageNumber, pageSize = 8, groupSelector) map (assetComplex => {
        val error = request.flash.get("error")
        Ok(views.html.container.asset.asset_overview(assetComplex.parentAssetType, assetComplex.allAssetTypes, assetComplex.children, pageNumber, error))
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
        val newAssetForm = NewAssetForm.form.fill(NewAssetForm.Data(Seq(), Seq(), Seq(), Seq()))
        val error = request.flash.get("error")
        val success = request.flash.get("success")
        newAssetEditorFactory(assetTypeId, newAssetForm, error, success)
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
          errorForm => newAssetEditorFactory(assetTypeId, errorForm),
          data => {
            assetService.addAsset(assetTypeId, data.values, data.maintainers, data.editors, data.viewers) map (_ => {
              Redirect(routes.AssetController.getNewAssetEditor(assetTypeId)).flashing("success" -> "Asset successfully created")
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
    for {
      groups <- groupService.getAllGroups
      typeData <- modelAssetService.getCompleteAssetType(assetTypeId)
    } yield {
      val (assetType, constraints) = typeData
      Ok(views.html.container.asset.new_asset_editor(assetType,
        assetService.getAssetPropertyKeys(constraints),
        assetService.getObligatoryPropertyKeys(constraints),
        groups,
        form, errmsg, succmsg))
    }
  } recoverWith {
    case e =>
      logger.error(e.getMessage, e)
      Future.successful(Redirect(routes.AssetController.index()).flashing("error" -> e.getMessage))
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
        assetEditorFactory(assetTypeId, assetId, None)
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
          errorForm => assetEditorFactory(assetTypeId, assetId, Option(errorForm)),
          data => {
            assetService.updateAsset(assetId, data.values, data.maintainers, data.editors, data.viewers) flatMap (_ => {
              assetEditorFactory(assetTypeId, assetId, Option(NewAssetForm.form.fill(data)), None, Option("Changes saved successfully"))
            }) recoverWith {
              case e: Throwable =>
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
          Redirect(routes.AssetController.getAssets(assetTypeId, 0))
          ) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Redirect(routes.AssetController.getAssetEditor(assetTypeId, assetId)).flashing("error" -> e.getMessage))
        }
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
  private def assetEditorFactory(assetTypeId: Long, assetId: Long, form: Option[Form[NewAssetForm.Data]],
                                 msg: Option[String] = None, successMsg: Option[String] = None)
                                (implicit request: Request[AnyContent], ticket: Ticket): Future[Result] = {
    for {
      extendedAsset <- assetService.getAsset(assetId)
      typeData <- modelAssetService.getCompleteAssetType(extendedAsset.asset.typeId)
      groups <- groupService.getAllGroups
    } yield {
      val (assetType, constraints) = typeData
      val editForm = if (form.isDefined) form.get else NewAssetForm.form.fill(
        NewAssetForm.Data(
          extendedAsset.properties.map(_.value),
          extendedAsset.viewers.maintainers.toSeq.map(_.name),
          extendedAsset.viewers.editors.toSeq.map(_.name),
          extendedAsset.viewers.viewers.toSeq.map(_.name)))

      Ok(views.html.container.asset.asset_editor(assetType,
        extendedAsset,
        assetService.getAssetPropertyKeys(constraints),
        assetService.getObligatoryPropertyKeys(constraints),
        groups,
        editForm, msg, successMsg))
    }
  } recoverWith {
    case e =>
      logger.error(e.getMessage, e)
      Future.successful(Redirect(routes.AssetController.getAssets(assetTypeId, 0)).flashing("error" -> e.getMessage))
  }

}


