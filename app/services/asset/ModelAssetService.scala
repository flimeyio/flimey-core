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

package services.asset

import com.google.inject.Inject
import db.asset.{AssetConstraintRepository, AssetTypeRepository}
import model.asset.AssetType
import model.auth.Ticket
import model.generic.Constraint
import play.api.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic for AssetTypes and their Constraints.
 * This class is normally used by dependency injection inside controller endpoints.
 *
 * @param assetTypeRepository       injected db interface for AssetTypes
 * @param assetConstraintRepository injected db interface for (Asset)Constraints
 */
class ModelAssetService @Inject()(assetTypeRepository: AssetTypeRepository, assetConstraintRepository: AssetConstraintRepository) extends Logging{

  /**
   * Add a new AssetType.
   * ID must be 0 and name must be unique (else the future will fail).
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param assetType new AssetType
   * @return new db id future
   */
  def addAssetType(assetType: AssetType)(implicit ticket: Ticket): Future[Long] = {
    try {
      assetTypeRepository.add(assetType)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all AssetTypes.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @return sequence of all AssetTypes future (can be empty)
   */
  def getAllAssetTypes(implicit ticket: Ticket): Future[Seq[AssetType]] = {
    try {
      assetTypeRepository.getAll
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get an AssetType by its ID.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @return AssetType future or None
   */
  def getAssetType(id: Long)(implicit ticket: Ticket): Future[Option[AssetType]] = {
    try {
      assetTypeRepository.get(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  def getCompleteAssetType(id: Long)(implicit ticket: Ticket): Future[(Option[AssetType], Seq[Constraint])] = {
    try {
      assetTypeRepository.getComplete(id)
    } catch {
      case e: Throwable => {
        logger.error("getCompleteAssetType", e)
        Future.failed(e)
      }
    }
  }

  /**
   * Get an AssetType by its value field.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @return AssetType future or None
   */
  def getAssetTypeByValue(value: String)(implicit ticket: Ticket): Future[Option[AssetType]] = {
    try {
      getAllAssetTypes flatMap(types => Future.successful(types.find(_.value == value)))
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Update an already existing AssetType entity.<br />
   * To change the 'active' property to true, the Constraint model must be valid!
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param assetType to update
   * @return result status future
   */
  def updateAssetType(assetType: AssetType)(implicit ticket: Ticket): Future[Int] = {
    try {
      if (assetType.active) {
        getConstraintsOfAsset(assetType.id) flatMap (constraints => {
          val status = AssetLogic.isAssetConstraintModel(constraints)
          if (!status.valid) status.throwError
          assetTypeRepository.update(assetType)
        })
      } else {
        assetTypeRepository.update(assetType)
      }
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all Constraints associated to an AssetType.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param id of the AssetType
   * @return sequence of (Asset)Constraints
   */
  def getConstraintsOfAsset(id: Long)(implicit ticket: Ticket): Future[Seq[Constraint]] = {
    try {
      assetConstraintRepository.getAssociated(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get an (Asset)Constraint by its ID.
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param id of the Constraint
   * @return Constraint future or None
   */
  def getConstraint(id: Long)(implicit ticket: Ticket): Future[Option[Constraint]] = {
    try {
      assetConstraintRepository.get(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete an (Asset)Constraint by its ID.<br />
   * By deleting a Constraint, the associated AssetType model must stay valid.
   * If the removal of the Constraint will invalidate the model, the future will fail.<br />
   * <br />
   * Not implemented yet:
   * The removal of a 'HasProperty' Constraint leads to the removal of all corresponding Asset data properties!<br />
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param id of the (Asset)Constraint
   * @return result future
   */
  def deleteConstraint(id: Long)(implicit ticket: Ticket): Future[Int] = {
    try {
      getConstraint(id) flatMap (constraint => {
        if (constraint.isEmpty) throw new Exception("No such Constraint found")
        getAssetType(constraint.get.typeId) flatMap (assetType => {
          if (assetType.isEmpty) throw new Exception("No corresponding AssetType found")
          getConstraintsOfAsset(assetType.get.id) flatMap (constraints => {
            val status = AssetLogic.isAssetConstraintModel(constraints.filter(c => c.id != id))
            if (!status.valid) status.throwError
            //FIXME alter Asset entities in transaction
            assetConstraintRepository.delete(id)
          })
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Add an (Asset)Constraint to an AssetType.<br />
   * ID must be 0 (else the future will fail).
   * If the addition of the Constraint will invalidate the model, the future will fail.<br />
   * <br />
   * Not implemented yet:
   * The addition of a 'MustBeDefined' Constraint will modify all associated Assets by introducing the rules default value!<br />
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param assetConstraint to add
   * @return result future
   */
  def addConstraint(assetConstraint: Constraint)(implicit ticket: Ticket): Future[Long] = {
    try {
      val processedConstrained = AssetLogic.preprocessConstraint(assetConstraint)
      val constraintStatus = AssetLogic.isValidConstraint(processedConstrained)
      if (!constraintStatus.valid) constraintStatus.throwError
      getConstraintsOfAsset(assetConstraint.typeId) flatMap { i =>
        val modelStatus = AssetLogic.isAssetConstraintModel(i :+ processedConstrained)
        if (!modelStatus.valid) modelStatus.throwError
        //FIXME alter Asset entities in transaction
        assetConstraintRepository.add(processedConstrained)
      }
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete an AssetType.<br />
   * This operation will also delete all associated Constraints and all Assets which have this type!<br />
   * <br />
   * Not implemented yet:
   * If a SubjectType model will become invalid because of this operation, nothing will be performed and the future will fail.
   * In this case, the affected SubjectTypes must be modified before.<br />
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param id of the AssetType
   * @return result future
   */
  def deleteAssetType(id: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      //FIXME validate SubjectTypes
      assetTypeRepository.delete(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all AssetTypes, a specific AssetType and its Constraints at once.<br />
   * This operation is just a future comprehension of different service methods to reduce load in the controller.<br />
   * <br />
   * This is a safe implementation and can be used by controller classes.
   *
   * @param id of an AssetType
   * @return Tuple of all AssetTypes, a specific AssetType and its Constraint
   */
  def getCombinedAssetEntity(id: Long)(implicit ticket: Ticket): Future[(Seq[AssetType], Option[AssetType], Seq[Constraint])] = {
    try {
      (for {
        assetTypes <- getAllAssetTypes
        constraints <- getConstraintsOfAsset(id)
      } yield (assetTypes, constraints)) map (res => {
        (res._1, res._1.find(p => p.id == id), res._2)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

}
