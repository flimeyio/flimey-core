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
import modules.asset.service.{AssetService, ModelAssetService}
import modules.auth.model.Ticket
import modules.core.formdata.{EntityForm, SelectValueForm}
import modules.user.service.GroupService
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The AssetController responsible for all endpoints regarding [[modules.asset.model.Asset Asset]] creation and management.
 *
 * @param cc                 injected ControllerComponents
 * @param assetService       injected AssetService for business logic
 * @param modelAssetService  injected ModelAssetService for business logic regarding [[modules.core.model.EntityType EntityTypes]]
 * @param withAuthentication injected AuthenticationFilter
 * @param groupService       injected GroupService for [[modules.user.model.Group Group]] management
 */
@Singleton
class AssetController @Inject()(cc: ControllerComponents, withAuthentication: AuthenticationFilter,
                                assetService: AssetService, modelAssetService: ModelAssetService, groupService: GroupService) extends
  AbstractController(cc) with I18nSupport with Logging with Authentication {

  /**
   * Endpoint to show the [[modules.asset.model.Asset Asset]] overview page.<br />
   * No [[modules.core.model.EntityType EntityTypes]] is initially selected.
   *
   * @return asset overview page
   */
  def index: Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        modelAssetService.getAllVersions() map (types => {
          val error = request.flash.get("error")
          Ok(views.html.container.asset.asset_overview(None, types, Seq(), 0, error))
        }) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Ok(views.html.container.asset.asset_overview(None, Seq(), Seq(), 0, Option(e.getMessage))))
        }
      }
    }

  /**
   * FIXME
   *
   * @return
   */
  def searchAssets: Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        modelAssetService.getAllVersions() map (types => {
          Ok(views.html.container.asset.asset_overview(None, types, Seq(), 0, None))
        })
      }
    }

  /**
   * Endpoint to change the shown [[modules.core.model.EntityType EntityType]] in the selection.
   * <p> Resets the search query an other filters.
   * <p> Does not generate a result but redirects to getAssetOfType() with updated EntityType index parameter.
   *
   * @return redirect to getAssetsOfType or empty overview page with an error message
   */
  def changeAssetType: Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        SelectValueForm.form.bindFromRequest fold(
          errorForm => {
            Future.successful(Redirect(routes.AssetController.index()).flashing("error" -> "No such Asset Type found"))
          },
          data => {
            val assetTypeValue = data.value
            modelAssetService.getTypeByValue(assetTypeValue) flatMap (assetType => {
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
   * Endpoint to get a number of [[modules.asset.model.Asset Assets]] based on multiple query parameters.
   * <p> In every case, only Assets which can be accessed based on the Ticket can be selected.
   *
   * @param assetTypeId   id of the [[modules.core.model.TypeVersion TypeVersion]] the Assets must have
   * @param pageNumber    number of the selection window - see [[modules.asset.service.AssetService#getAssets]]
   * @param groupSelector string containing [[modules.user.model.Group Group]] ids which the Assets can have (filter) in form "id,id,..."
   * @return asset overview page with listed assets given by the query parameters
   */
  def getAssets(assetTypeId: Long, pageNumber: Int, groupSelector: Option[String] = None):
  Action[AnyContent] = withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
    withTicket { implicit ticket =>
      assetService.getAssetComplex(assetTypeId, pageNumber, pageSize = 20, groupSelector) map (assetComplex => {
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
   * Endpoint to get an editor to create new [[modules.asset.model.Asset Assets]].
   * <p>The editor will only accept Assets of the previously selected (currently active) [[modules.core.model.EntityType EntityType]].
   *
   * @param assetTypeId id of the EntityType
   * @return new asset editor page
   */
  def getNewAssetEditor(assetTypeId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        val newAssetForm = EntityForm.form.fill(EntityForm.Data(Seq(), Seq(), Seq(), Seq()))
        val error = request.flash.get("error")
        val success = request.flash.get("success")
        newAssetEditorFactory(assetTypeId, newAssetForm, error, success)
      }
    }

  /**
   * Endpoint to add a new [[modules.asset.model.Asset Asset]].
   * <p> The Asset must match the model of the selected EntityType.
   * <p> The incoming form data seq must be in the same order as the previously sent property keys.
   *
   * @param assetTypeId id of the [[modules.core.model.EntityType EntityType]]
   * @return new asset editor page (clean or with errors)
   */
  def addNewAsset(assetTypeId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        EntityForm.form.bindFromRequest fold(
          errorForm => newAssetEditorFactory(assetTypeId, errorForm),
          data => {
            assetService.addAsset(assetTypeId, data.values, data.maintainers, data.editors, data.viewers) map (_ => {
              Redirect(routes.AssetController.getNewAssetEditor(assetTypeId)).flashing("success" -> "Asset successfully created")
            }) recoverWith {
              case e =>
                logger.error(e.getMessage, e)
                val newAssetForm = EntityForm.form.fill(data)
                newAssetEditorFactory(assetTypeId, newAssetForm, Option(e.getMessage))
            }
          })
      }
    }

  /**
   * Helper function to build a 'new asset editor' view based on different configuration parameters.
   * <p> The editor will always be configured with the newest available [[modules.core.model.TypeVersion TypeVersion]]
   * of the given [[modules.core.model.EntityType EntityType]].
   *
   * @param assetTypeId id of the [[modules.core.model.EntityType EntityType]]
   * @param form        NewAssetForm, which can be already filled
   * @param errmsg      optional error message
   * @param succmsg     optional positive message
   * @param request     implicit request context
   * @return new asset editor page
   */
  private def newAssetEditorFactory(assetTypeId: Long, form: Form[EntityForm.Data], errmsg: Option[String] = None, succmsg: Option[String] = None)
                                   (implicit request: Request[AnyContent], ticket: Ticket): Future[Result] = {
    for {
      groups <- groupService.getAllGroups
      typeData <- modelAssetService.getLatestExtendedType(assetTypeId)
    } yield {
      Ok(views.html.container.asset.new_asset_editor(typeData.entityType,
        assetService.getAssetPropertyKeys(typeData.constraints),
        assetService.getObligatoryPropertyKeys(typeData.constraints),
        groups,
        form, errmsg, succmsg))
    }
  } recoverWith {
    case e =>
      logger.error(e.getMessage, e)
      Future.successful(Redirect(routes.AssetController.index()).flashing("error" -> e.getMessage))
  }

  /**
   * Endpoint to get the [[modules.asset.model.Asset Asset]] editor to edit an existing Asset.
   * <p> The Asset to edit must part of the current overview selection.
   *
   * @param assetTypeVersionId id of the [[modules.core.model.EntityType EntityTypes]] [[modules.core.model.TypeVersion TypeVersion]]
   * @param assetId            id of the Asset to edit
   * @return editor page with preloaded asset data
   */
  def getAssetEditor(assetTypeVersionId: Long, assetId: Long): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        modelAssetService.getVersionedType(assetTypeVersionId) flatMap (typeOption => {
          if (typeOption.isDefined) {
            assetEditorFactory(typeOption.get.entityType.id, assetId, None)
          } else {
            Future.successful(Redirect(routes.AssetController.index()).flashing("error" -> "No such asset type found"))
          }
        }) recoverWith {
          case e =>
            logger.error(e.getMessage, e)
            Future.successful(Redirect(routes.AssetController.index()).flashing("error" -> e.getMessage))
        }
      }
    }

  /**
   * Endpoint to post (update) the data of the currently edited [[modules.asset.model.Asset Asset]].
   *
   * @param assetTypeId id of the [[modules.core.model.EntityType EntityType]] (used for redirects)
   * @param assetId     id of the Asset to edit
   * @param msg         optional error message
   * @return editor page with success or error message
   */
  def postAsset(assetTypeId: Long, assetId: Long, msg: Option[String] = None): Action[AnyContent] =
    withAuthentication.async { implicit request: AuthenticatedRequest[AnyContent] =>
      withTicket { implicit ticket =>
        EntityForm.form.bindFromRequest fold(
          errorForm => assetEditorFactory(assetTypeId, assetId, Option(errorForm)),
          data => {
            assetService.updateAsset(assetId, data.values, data.maintainers, data.editors, data.viewers) flatMap (_ => {
              assetEditorFactory(assetTypeId, assetId, Option(EntityForm.form.fill(data)), None, Option("Changes saved successfully"))
            }) recoverWith {
              case e: Throwable =>
                logger.error(e.getMessage, e)
                val newAssetForm = EntityForm.form.fill(data)
                assetEditorFactory(assetTypeId, assetId, Option(newAssetForm), Option(e.getMessage))
            }
          })
      }
    }

  /**
   * Endpoint to delete an [[modules.asset.model.Asset Asset]].
   * <p> The Asset is deleted permanently and can not be restored!
   *
   * @param assetTypeId id of the parent [[modules.core.model.EntityType EntityType]] (used for redirects)
   * @param assetId     id of the Asset to delete
   * @param msg         optional error message
   * @return redirect to asset overview page with currently active type
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
   * @param assetTypeId id of the [[modules.core.model.EntityType EntityType]]
   * @param assetId     id of the [[modules.asset.model.Asset Asset]] to edit
   * @param form        optional prepared form data
   * @param msg         optional error message
   * @param request     implicit request context
   * @return asset editor page with prepared data
   */
  private def assetEditorFactory(assetTypeId: Long, assetId: Long, form: Option[Form[EntityForm.Data]],
                                 msg: Option[String] = None, successMsg: Option[String] = None)
                                (implicit request: Request[AnyContent], ticket: Ticket): Future[Result] = {
    for {
      extendedAsset <- assetService.getAsset(assetId)
      typeData <- modelAssetService.getExtendedType(extendedAsset.asset.typeVersionId)
      groups <- groupService.getAllGroups
    } yield {
      val editForm = if (form.isDefined) form.get else EntityForm.form.fill(
        EntityForm.Data(
          extendedAsset.properties.map(_.value),
          extendedAsset.viewers.maintainers.toSeq.map(_.name),
          extendedAsset.viewers.editors.toSeq.map(_.name),
          extendedAsset.viewers.viewers.toSeq.map(_.name)))

      Ok(views.html.container.asset.asset_editor(typeData.entityType,
        extendedAsset,
        assetService.getAssetPropertyKeys(typeData.constraints),
        assetService.getObligatoryPropertyKeys(typeData.constraints),
        groups,
        editForm, msg, successMsg))
    }
  } recoverWith {
    case e =>
      logger.error(e.getMessage, e)
      Future.successful(Redirect(routes.AssetController.getAssets(assetTypeId, 0)).flashing("error" -> e.getMessage))
  }

}