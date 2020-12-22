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

package asset.service

import asset.model.{Asset, AssetComplex, AssetConstraint, AssetProperty}
import asset.repository.{AssetPropertyRepository, AssetRepository, AssetTypeRepository}
import auth.model.Ticket
import com.google.inject.Inject
import util.assertions.RoleAssertion

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic for Assets and their Properties.
 * This class is normally used by dependency injection inside controller endpoints.
 *
 * @param assetTypeRepository     injected db interface for AssetTypes
 * @param assetRepository         injected db interface for Assets
 * @param assetPropertyRepository injected db interface for (Asset)Properties
 */
class AssetService @Inject()(assetTypeRepository: AssetTypeRepository,
                             assetRepository: AssetRepository,
                             assetPropertyRepository: AssetPropertyRepository,
                             modelAssetService: ModelAssetService) extends RoleAssertion {

  /**
   * Add a new Asset.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param typeId       id of the AssetType
   * @param propertyData of the new Asset (must complete the AssetType model)
   * @return db result future
   */
  def addAsset(typeId: Long, propertyData: Seq[String])(implicit ticket: Ticket): Future[Unit] = {
    try {
      assertWorker
      assetTypeRepository.getComplete(typeId) flatMap (typeData => {
        val (head, constraints) = typeData
        if (!(head.isDefined && head.get.active)) throw new Exception("The selected Asset Type is not active")
        val properties = AssetLogic.derivePropertiesFromRawData(constraints, propertyData)
        val configurationStatus = AssetLogic.isModelConfiguration(constraints, properties)
        if (configurationStatus.valid) {
          assetRepository.add(Asset(0, typeId), properties)
        } else {
          configurationStatus.throwError
        }
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all Assets.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param typeId id of the AssetType
   * @return sequence of all AssetTypes future (can be empty)
   */
  def getAllAssetsOfType(typeId: Long)(implicit ticket: Ticket): Future[Seq[(Asset, Seq[AssetProperty])]] = {
    try {
      assertWorker
      assetRepository.getAll(typeId) map (data =>
        data.filter(elem => {
          elem._2.head.isDefined
        }).map(d => {
          (d._1, d._2.map(_.get))
        }))
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * A more convenient implementation of getAllAssetTypes() combined with getAllAssetsOfType().
   * Basically just a comprehension of those two functions with an additional error check.<br />
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param typeId id of the selected AssetType
   * @return 3-Tuple (the selected AssetType, all AssetTypes, all Assets of the selected Type)
   */
  def getAssetComplex(typeId: Long)(implicit ticket: Ticket): Future[AssetComplex] = {
    try {
      assertWorker
      modelAssetService.getAllAssetTypes flatMap (types => {
        val selectedAssetType = types.find(_.id == typeId)
        if (selectedAssetType.isDefined) {
          getAllAssetsOfType(typeId) map (assetData => {
            AssetComplex(selectedAssetType, types, assetData)
          })
        } else {
          throw new Exception("No such Asset Type found")
        }
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Update an already existing Asset entity.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param assetId            of the parent Asset
   * @param propertyUpdateData of the Asset (complete, but can contain updates)
   * @return result status future
   */
  def updateAssetProperties(assetId: Long, propertyUpdateData: Seq[String])(implicit ticket: Ticket): Future[Seq[Int]] = {
    try {
      assertWorker
      assetRepository.get(assetId) flatMap (assetData => {
        val (asset, properties) = assetData
        if (asset.isEmpty) throw new Exception("No such Asset to update")
        //basically only removes the None option, if no properties where in the old config, else all elements are defined
        val oldConfig = properties.filter(_.isDefined).map(_.get)
        val newConfig = AssetLogic.mapConfigurations(oldConfig, propertyUpdateData)

        assetTypeRepository.getComplete(asset.get.typeId) flatMap (typeData => {
          val (head, constraints) = typeData
          if (!(head.isDefined && head.get.active)) throw new Exception("The selected Asset Type is not active")
          val configurationStatus = AssetLogic.isModelConfiguration(constraints, newConfig)
          if (!configurationStatus.valid) configurationStatus.throwError
          assetPropertyRepository.update(newConfig)
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete an Asset.<br />
   * <br />
   * Not implemented yet:
   * If a Subject will become invalid because of this operation, nothing will be performed and the future will fail.
   * In this case, the affected Subjects must be modified before.<br />
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param id of the Asset
   * @return result future
   */
  def deleteAsset(id: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      assertWorker
      //FIXME validate Subjects
      assetRepository.delete(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Forwards to same method of AssetLogic.<br />
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param constraints model of an AssetType
   * @return tuple2 seq (property key, data type)
   */
  def getAssetPropertyKeys(constraints: Seq[AssetConstraint])(implicit ticket: Ticket): Seq[(String, String)] = {
    assertWorker
    AssetLogic.getAssetPropertyKeys(constraints)
  }

  /**
   * Forwards to same method of AssetLogic<br />
   * <br />
   *
   * @param constraints model of an AssetType
   * @return map of (property key -> default value)
   */
  def getObligatoryPropertyKeys(constraints: Seq[AssetConstraint])(implicit ticket: Ticket): Map[String, String] = {
    assertWorker
    AssetLogic.getObligatoryPropertyKeys(constraints)
  }

}
