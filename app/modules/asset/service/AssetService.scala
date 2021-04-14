/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020-2021 Karl Kegel
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

package modules.asset.service

import com.google.inject.Inject
import modules.asset.model.{Asset, AssetTypeCombination, ExtendedAsset}
import modules.asset.repository.AssetRepository
import modules.auth.model.Ticket
import modules.auth.util.RoleAssertion
import modules.core.model.Constraint
import modules.core.repository.{FlimeyEntityRepository, PropertyRepository, TypeRepository}
import modules.user.model.GroupStats
import modules.user.service.GroupService
import modules.user.util.ViewerAssertion

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic for [[modules.asset.model.Asset Assets]] and their
 * [[modules.core.model.Property Properties]].
 * This class is normally used by dependency injection inside controller endpoints.
 *
 * @param typeRepository     injected db interface for [[modules.core.model.EntityType EntityTypes]]
 * @param assetRepository    injected db interface for Assets
 * @param propertyRepository injected db interface for Properties
 * @param entityRepository   injected db interface for [[modules.core.model.FlimeyEntity Entities]]
 * @param modelAssetService  injected service class for Asset specific EntityType models
 * @param groupService       injected service class to access [[modules.user.model.Group Group]] functionality
 */
class AssetService @Inject()(typeRepository: TypeRepository,
                             assetRepository: AssetRepository,
                             propertyRepository: PropertyRepository,
                             entityRepository: FlimeyEntityRepository,
                             modelAssetService: ModelAssetService,
                             groupService: GroupService) {

  /**
   * Add a new [[modules.asset.model.Asset Asset]].
   * <p> Fails without WORKER rights.
   * <p> Note: a [[modules.user.model.User User]] (defined by his ticket) can create Assets he is unable to access himself
   * (by assigning other [[modules.user.model.Group Groups]])
   * <p> Invalid and duplicate names in maintainers, editors and viewers is filtered out and does not lead to exceptions.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param typeId       id of the EntityType
   * @param propertyData of the new Asset (must complete the AssetType model)
   * @param maintainers  names of Groups to serve as maintainers
   * @param editors      names of Groups to serve as editors
   * @param viewers      names of Groups to serve as viewers
   * @param ticket       implicit authentication ticket
   * @return Future[Unit]
   */
  def addAsset(typeId: Long, propertyData: Seq[String], maintainers: Seq[String], editors: Seq[String],
               viewers: Seq[String])(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertWorker
      modelAssetService.getLatestExtendedType(typeId) flatMap (extendedEntityType => {
        if (!extendedEntityType.entityType.active) throw new Exception("The selected Asset Type is not active")
        val properties = AssetLogic.derivePropertiesFromRawData(extendedEntityType.constraints, propertyData)
        val configurationStatus = AssetLogic.isModelConfiguration(extendedEntityType.constraints, properties)
        if (!configurationStatus.valid) configurationStatus.throwError
        groupService.getAllGroups map (allGroups => {
          val aViewers = AssetLogic.deriveViewersFromData(maintainers :+ GroupStats.SYSTEM_GROUP, editors, viewers, allGroups)
          assetRepository.add(Asset(0, 0, extendedEntityType.version.id), properties, aViewers)
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Update an [[modules.asset.model.Asset Assets]] properties and [[modules.core.model.Viewer Viewers]].
   * <p> All [[modules.core.model.Property Properties]] (if updated or not) must be passed, else the configuration
   * can not be verified.
   * <p> All Viewers (old AND new ones) must be passed as string. Old viewers that are not passed will be deleted.
   * Invalid and duplicate Viewer names (Group names) are filtered out. Only the highest role is applied per Viewer.
   * The SYSTEM Group can not be removed as MAINTAINER.
   * <p> Fails without WORKER rights.
   * <p> If Properties are changed, EDITOR rights are required.
   * <p> If Viewers are change, MAINTAINER rights are required.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param assetId            id of the Asset to update
   * @param propertyUpdateData all Properties of the changed Asset (can contain updated values)
   * @param maintainers        all (old and new) Group names of Viewers with role MAINTAINER
   * @param editors            all (old and new) Group names of Viewers with role EDITOR
   * @param viewers            all (old and new) Group names of Viewers with role VIEWER
   * @param ticket             implicit authentication ticket
   * @return Future[Unit]
   */
  def updateAsset(assetId: Long, propertyUpdateData: Seq[String], maintainers: Seq[String], editors: Seq[String],
                  viewers: Seq[String])(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertWorker
      getAsset(assetId) flatMap (extendedAsset => {

        //Check if the User can edit this Asset
        ViewerAssertion.assertEdit(extendedAsset.viewers)

        //Parse updated properties and verify the configuration
        val properties = extendedAsset.properties
        val oldConfig = properties
        val newConfig = AssetLogic.mapConfigurations(oldConfig, propertyUpdateData)

        //check if the AssetType of the Asset is active (else it can not be edited)
        typeRepository.getExtended(extendedAsset.asset.typeVersionId) flatMap (extendedEntityType => {
          if (extendedEntityType.isEmpty || !extendedEntityType.get.entityType.active) throw new Exception("The selected Asset Type is not defined or active")
          val configurationStatus = AssetLogic.isModelConfiguration(extendedEntityType.get.constraints, newConfig)
          if (!configurationStatus.valid) configurationStatus.throwError

          groupService.getAllGroups flatMap (groups => {

            val (viewersToDelete, viewersToInsert) = AssetLogic.getViewerChanges(
              maintainers.toSet + GroupStats.SYSTEM_GROUP,
              editors.toSet,
              viewers.toSet,
              extendedAsset.viewers,
              groups,
              extendedAsset.asset.entityId)

            if (viewersToDelete.nonEmpty || viewersToInsert.nonEmpty) {
              ViewerAssertion.assertMaintain(extendedAsset.viewers)
            }
            entityRepository.update(newConfig, viewersToDelete, viewersToInsert)
          })
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get an [[modules.asset.model.ExtendedAsset ExtendedAsset]] by its id.
   * <p> A [[modules.user.model.User User]] (given by his ticket) can only request assets he has access rights to.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param assetId id of the [[modules.asset.model.Asset Asset]] to fetch
   * @param ticket  implicit authentication ticket
   * @return Future[ExtendedAsset]
   */
  def getAsset(assetId: Long)(implicit ticket: Ticket): Future[ExtendedAsset] = {
    try {
      RoleAssertion.assertWorker
      val accessedGroupIds = ticket.accessRights.getAllViewingGroupIds
      assetRepository.get(assetId, accessedGroupIds) map (assetOption => {
        if (assetOption.isEmpty) throw new Exception("No such asset found")
        assetOption.get
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get a number of [[modules.asset.model.ExtendedAsset ExtendedAssets]] defined by multiple query parameters.
   * <p> Only Assets the given [[modules.user.model.User User]] (by ticket) can access are returned.
   * <p> Only a specified range of Assets are returned, given by the pageNumber.
   * The [[modules.asset.model.Asset Assets]] returned by a specific parameter combination (also pageNumber) do not stay
   * constant but can change due to Asset deletion and Management!
   * <p> Fails without WORKER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param typeId id of the [[modules.core.model.EntityType EntityType]] every Asset must have
   * @param pageNumber    number of the Asset page (starting with 0)
   * @param pageSize      maximum size of a page (max result size)
   * @param groupSelector groups which must contain the returned Assets (must be partition of ticket Groups)
   * @param ticket        implicit authentication ticket
   * @return Future Seq[ExtendedAsset]
   */
  def getAssets(typeId: Long, pageNumber: Int, pageSize: Int, groupSelector: Option[String] = None)
               (implicit ticket: Ticket): Future[Seq[ExtendedAsset]] = {
    try {
      RoleAssertion.assertWorker
      var accessedGroupIds = ticket.accessRights.getAllViewingGroupIds
      if (groupSelector.isDefined) {
        val selectedGroups = AssetLogic.splitNumericList(groupSelector.get)
        accessedGroupIds = accessedGroupIds.filter(!selectedGroups.contains(_))
      }
      if (pageNumber < 0) throw new Exception("Page number must be positive")
      val offset = pageNumber * pageSize
      val limit = pageSize
      assetRepository.getAssetSubset(accessedGroupIds, typeId, limit, offset)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get a number of [[modules.asset.model.Asset Assets]] defined by multiple query parameters and information on all
   * [[modules.core.model.EntityType EntityTypes]] as [[modules.asset.model.AssetTypeCombination AssetTypeCombinations]]
   * <p> This method calls [[modules.asset.service.ModelAssetService#getAllTypes]] (see there for more information)
   * <p> This method calls [[modules.asset.service.AssetService#getAssets]] (see there for more information)
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param typeId        id of the EntityType every Asset must have
   * @param pageNumber    number of the Asset page (starting with 0)
   * @param pageSize      maximum size of a page (max result size)
   * @param groupSelector [[modules.user.model.Group Groups]] which must contain the returned Assets (must be partition of ticket Groups)
   * @param ticket        implicit authentication ticket
   * @return Future[AssetTypeCombination]
   */
  def getAssetComplex(typeId: Long, pageNumber: Int, pageSize: Int, groupSelector: Option[String] = None)
                     (implicit ticket: Ticket): Future[AssetTypeCombination] = {
    try {
      RoleAssertion.assertWorker
      modelAssetService.getAllTypes() flatMap (types => {
        val selectedAssetType = types.find(_.id == typeId)
        if (selectedAssetType.isDefined) {
          getAssets(typeId, pageNumber, pageSize, groupSelector) map (assetData => {
            AssetTypeCombination(selectedAssetType, types, assetData)
          })
        } else {
          throw new Exception("No such Asset Type Version found")
        }
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete an [[modules.asset.model.Asset Asset]].
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without MAINTAINER rights
   *
   * @param id     of the Asset
   * @param ticket implicit authentication ticket
   * @return Future[Unit]
   */
  def deleteAsset(id: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertWorker
      getAsset(id) flatMap (extendedAsset => {
        ViewerAssertion.assertMaintain(extendedAsset.viewers)
        //FIXME validate Subjects
        assetRepository.delete(extendedAsset.asset)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Forwards to same method of [[modules.asset.service.AssetLogic AssetLogic]].
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param constraints model of an [[modules.core.model.TypeVersion TypeVersion]]
   * @param ticket      implicit authentication ticket
   * @return Seq[(String, String)] (property key, data type)
   */
  def getAssetPropertyKeys(constraints: Seq[Constraint])(implicit ticket: Ticket): Seq[(String, String)] = {
    RoleAssertion.assertWorker
    AssetLogic.getPropertyKeys(constraints)
  }

  /**
   * Forwards to same method of [[modules.asset.service.AssetLogic AssetLogic]].
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param constraints model of an [[modules.core.model.TypeVersion TypeVersion]]
   * @param ticket      implicit authentication ticket
   * @return Map[String, String] (property key -> default value)
   */
  def getObligatoryPropertyKeys(constraints: Seq[Constraint])(implicit ticket: Ticket): Map[String, String] = {
    RoleAssertion.assertWorker
    AssetLogic.getObligatoryPropertyKeys(constraints)
  }

}
