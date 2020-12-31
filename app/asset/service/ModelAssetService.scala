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

import asset.model.{AssetConstraint, AssetType}
import asset.repository.{AssetConstraintRepository, AssetTypeRepository}
import auth.model.Ticket
import auth.util.RoleAssertion
import com.google.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic for AssetTypes and their Constraints.
 * This class is normally used by dependency injection inside controller endpoints.
 *
 * @param assetTypeRepository       injected db interface for AssetTypes
 * @param assetConstraintRepository injected db interface for (Asset)Constraints
 */
class ModelAssetService @Inject()(assetTypeRepository: AssetTypeRepository, assetConstraintRepository: AssetConstraintRepository) {

  /**
   * Add a new AssetType.
   * <p> ID must be 0 and name must be unique (else the future will fail).
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param assetType new AssetType
   * @param ticket    implicit authentication ticket
   * @return Future[Long]
   */
  def addAssetType(assetType: AssetType)(implicit ticket: Ticket): Future[Long] = {
    try {
      RoleAssertion.assertModeler
      assetTypeRepository.add(assetType)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all AssetTypes.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param ticket implicit authentication ticket
   * @return Future Seq[AssetType]
   */
  def getAllAssetTypes(implicit ticket: Ticket): Future[Seq[AssetType]] = {
    try {
      RoleAssertion.assertWorker
      assetTypeRepository.getAll
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get an AssetType by its ID.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     od the AssetType
   * @param ticket implicit authentication ticket
   * @return Future Option[AssetType]
   */
  def getAssetType(id: Long)(implicit ticket: Ticket): Future[Option[AssetType]] = {
    try {
      RoleAssertion.assertWorker
      assetTypeRepository.get(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get a complete AssetType (Head + Constraints).
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     od the AssetType
   * @param ticket implicit authentication ticket
   * @return Future (AssetType, Seq[AssetConstraint])
   */
  def getCompleteAssetType(id: Long)(implicit ticket: Ticket): Future[(AssetType, Seq[AssetConstraint])] = {
    try {
      RoleAssertion.assertWorker
      assetTypeRepository.getComplete(id) map (assetTypeData => {
        val (assetType, constraints) = assetTypeData
        if (assetType.isEmpty) throw new Exception("Invalid asset type")
        (assetType.get, constraints)
      })
    } catch {
      case e: Throwable => Future.failed(e)

    }
  }

  /**
   * Get an AssetType by its value (name) field.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   * //TODO this can be extended to provide substring search results.
   *
   * @param value  value filed (name) of the searched AssetType
   * @param ticket implicit authentication ticket
   * @return Future Option[AssetType]
   */
  def getAssetTypeByValue(value: String)(implicit ticket: Ticket): Future[Option[AssetType]] = {
    try {
      RoleAssertion.assertWorker
      //FIXME this is not critical because there won't be many AssetTypes but filtering should be done in the repository.
      getAllAssetTypes flatMap (types => Future.successful(types.find(_.value == value)))
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Update an already existing AssetType entity. This includes 'value' (name) and 'active'.
   * <p> To change the 'active' property to true, the Constraint model must be valid!
   * <p> Fails without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param assetType to update hte 'value' and 'active' values
   * @param ticket    implicit authentication ticket
   * @return Future[Int]
   */
  def updateAssetType(assetType: AssetType)(implicit ticket: Ticket): Future[Int] = {
    try {
      RoleAssertion.assertModeler
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
   * <p> Fails without WORKER rights.
   * <p>This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the AssetType
   * @param ticket implicit authentication ticket
   * @return Future Seq[AssetConstraint]
   */
  def getConstraintsOfAsset(id: Long)(implicit ticket: Ticket): Future[Seq[AssetConstraint]] = {
    try {
      RoleAssertion.assertWorker
      assetConstraintRepository.getAssociated(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get an (Asset)Constraint by its ID.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the Constraint
   * @param ticket implicit authentication ticket
   * @return Future Option[AssetConstraint]
   */
  def getConstraint(id: Long)(implicit ticket: Ticket): Future[Option[AssetConstraint]] = {
    try {
      RoleAssertion.assertWorker
      assetConstraintRepository.get(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete an (Asset)Constraint by its ID.
   * <p> By deleting a Constraint, the associated AssetType model must stay valid.
   * If the removal of the Constraint will invalidate the model, the future will fail.
   * <p> <strong>The removal of a 'HasProperty' Constraint leads to the system wide removal of all corresponding
   * Asset data properties!</strong>
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the (Asset)Constraint to delete
   * @param ticket implicit authentication ticket
   * @return Future[Int]
   */
  def deleteConstraint(id: Long)(implicit ticket: Ticket): Future[Int] = {
    try {
      RoleAssertion.assertModeler
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
   * Add an (Asset)Constraint to an AssetType.
   * <p> ID must be 0 (else the future will fail). If the addition of the Constraint will invalidate the model,
   * the future will fail.
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   * //FIXME added has property constraint must add a new empty property to all assets of this type
   *
   * @param assetConstraint AssetConstraint to add (must already include the parent id)
   * @param ticket          implicit authentication ticket
   * @return Future[Long]
   */
  def addConstraint(assetConstraint: AssetConstraint)(implicit ticket: Ticket): Future[Long] = {
    try {
      RoleAssertion.assertModeler
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
   * Delete an AssetType.
   * <p> <strong> This operation will also delete all associated Constraints and all Assets which have this type! </strong>
   * <p> Fails without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the AssetType
   * @param ticket implicit authentication ticket
   * @return Future[Unit]
   */
  def deleteAssetType(id: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      assetTypeRepository.delete(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all AssetTypes, a specific AssetType and its Constraints at once.
   * <p> This operation is just a future comprehension of different service methods.
   * <p> Fails without WORKER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of an AssetType
   * @param ticket implicit authentication ticket
   * @return Future Tuple of all AssetTypes, a specific AssetType and its Constraints
   */
  def getCombinedAssetEntity(id: Long)(implicit ticket: Ticket): Future[(Seq[AssetType], Option[AssetType], Seq[AssetConstraint])] = {
    try {
      RoleAssertion.assertWorker
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
