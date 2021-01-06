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
import modules.asset.repository.AssetRepository
import modules.auth.model.Ticket
import modules.auth.util.RoleAssertion
import modules.core.model.{Constraint, ConstraintType, EntityType}
import modules.core.repository.{ConstraintRepository, TypeRepository}
import modules.core.service.EntityTypeService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic for AssetTypes and their Constraints.
 * This class is normally used by dependency injection inside controller endpoints.
 *
 * @param typeRepository       injected db interface for EntityTypes
 * @param constraintRepository injected db interface for Constraints
 * @param assetRepository      injected db interface for Assets and Asset specific model operations
 * @param entityTypeService    injected service to provide basic EntityType functionality
 */
class ModelAssetService @Inject()(typeRepository: TypeRepository,
                                  constraintRepository: ConstraintRepository,
                                  assetRepository: AssetRepository,
                                  entityTypeService: EntityTypeService) {

  /**
   * Get all AssetTypes.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param ticket implicit authentication ticket
   * @return Future Seq[AssetType]
   */
  def getAllAssetTypes()(implicit ticket: Ticket): Future[Seq[EntityType]] = {
    entityTypeService.getAllTypes(Option(AssetConstraintSpec.ASSET))
  }

  /**
   * Get an AssetType by its ID.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     od the AssetType
   * @param ticket implicit authentication ticket
   * @return Future Option[AssetType]
   */
  def getAssetType(id: Long)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    entityTypeService.getType(id, Option(AssetConstraintSpec.ASSET))
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
  def getCompleteAssetType(id: Long)(implicit ticket: Ticket): Future[(EntityType, Seq[Constraint])] = {
    entityTypeService.getCompleteType(id, Option(AssetConstraintSpec.ASSET))
  }

  /**
   * Get an AssetType by its value (name) field.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param value  value filed (name) of the searched AssetType
   * @param ticket implicit authentication ticket
   * @return Future Option[AssetType]
   */
  def getAssetTypeByValue(value: String)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    entityTypeService.getEntityTypeByValue(value, Option(AssetConstraintSpec.ASSET))
  }

  /**
   * Update an already existing AssetType entity. This includes 'value' (name) and 'active'.
   * <p> To change the 'active' property to true, the Constraint model must be valid!
   * <p> Fails without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id of the Type to update
   * @param ticket    implicit authentication ticket
   * @return Future[Int]
   */
  def updateAssetType(id: Long, value: String, active: Boolean)(implicit ticket: Ticket): Future[Int] = {
    try {
      RoleAssertion.assertModeler
      //FIXME the name should also be validated here
      if (active) {
        getConstraintsOfAssetType(id) flatMap (constraints => {
          val status = AssetLogic.isAssetConstraintModel(constraints)
          if (!status.valid) status.throwError
          typeRepository.update(EntityType(id, value, "", active))
        })
      } else {
        typeRepository.update(EntityType(id, value, "", active))
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
  def getConstraintsOfAssetType(id: Long)(implicit ticket: Ticket): Future[Seq[Constraint]] = {
    entityTypeService.getConstraintsOfEntityType(id, Option(AssetConstraintSpec.ASSET))
  }

  /**
   * Delete an AssetConstraint by its ID.
   * <p> By deleting a Constraint, the associated AssetType model must stay valid.
   * If the removal of the Constraint will invalidate the model, the future will fail.
   * <p> <strong>The removal of a 'HasProperty' Constraint leads to the system wide removal of all corresponding
   * Asset data properties!</strong>
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the AssetConstraint to delete
   * @param ticket implicit authentication ticket
   * @return Future[Int]
   */
  def deleteConstraint(id: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      entityTypeService.getConstraint(id) flatMap (constraintOption => {
        if (constraintOption.isEmpty) throw new Exception("No such Constraint found")
        val constraint = constraintOption.get
        getAssetType(constraint.typeId) flatMap (assetType => {
          if (assetType.isEmpty) throw new Exception("No corresponding AssetType found")
          getConstraintsOfAssetType(assetType.get.id) flatMap (constraints => {

            val status = AssetLogic.isAssetConstraintModel(constraints.filter(c => c.id != id))
            if (!status.valid) status.throwError

            if (constraint.c == ConstraintType.HasProperty) {
              assetRepository.deletePropertyConstraint(constraint)
            } else {
              constraintRepository.deleteConstraint(constraint.id) map (_ => Future.unit)
            }
          })
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Add an AssetConstraint to an AssetType.
   * <p> ID must be 0 (else the future will fail). If the addition of the Constraint will invalidate the model,
   * the future will fail.
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param c      String value of the ConstraintType
   * @param v1     first Constraint parameter
   * @param v2     second Constraint parameter
   * @param typeId id of the parent EntityType
   * @param ticket implicit authentication ticket
   * @return Future[Long]
   */
  def addConstraint(c: String, v1: String, v2: String, typeId: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      //FIXME the ConstraintType.find() needs a check before, the rulst can be empty and lead to a unspecified exception
      val assetConstraint = Constraint(0, ConstraintType.find(c).get, v1, v2, None, typeId)
      val constraintStatus = AssetLogic.isValidConstraint(assetConstraint)
      if (!constraintStatus.valid) constraintStatus.throwError
      getConstraintsOfAssetType(assetConstraint.typeId) flatMap { i =>
        val modelStatus = AssetLogic.isAssetConstraintModel(i :+ assetConstraint)
        if (!modelStatus.valid) modelStatus.throwError

        if (assetConstraint.c == ConstraintType.HasProperty) {
          assetRepository.addPropertyConstraint(assetConstraint)
        } else {
          constraintRepository.addConstraint(assetConstraint) map (_ -> Future.unit)
        }
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
      assetRepository.deleteAssetType(id)
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
  def getCombinedAssetEntity(id: Long)(implicit ticket: Ticket): Future[(Seq[EntityType], Option[EntityType], Seq[Constraint])] = {
    entityTypeService.getCombinedEntity(id, Option(AssetConstraintSpec.ASSET))
  }

}
