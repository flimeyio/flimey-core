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
import modules.asset.model.AssetConstraintSpec
import modules.asset.repository.AssetRepository
import modules.auth.model.Ticket
import modules.auth.util.RoleAssertion
import modules.core.model._
import modules.core.repository.{ConstraintRepository, TypeRepository}
import modules.core.service.{EntityTypeService, ModelEntityService}

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
                                  entityTypeService: EntityTypeService) extends ModelEntityService {

  /*
   * Note: AssetTypes are only allowed to have a single version!
   */

  /**
   * Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#getAllTypes]]
   * @param ticket implicit authentication ticket.
   * @return Future Seq[EntityType]
   */
  override def getAllTypes()(implicit ticket: Ticket): Future[Seq[EntityType]] = {
    entityTypeService.getAllTypes(Option(AssetConstraintSpec.ASSET))
  }

  /**
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#getAllVersions]]
   * @param ticket implicit authentication ticket
   * @return Future Seq[VersionedEntityType]
   */
  override def getAllVersions()(implicit ticket: Ticket): Future[Seq[VersionedEntityType]] = {
    entityTypeService.getAllVersions(Option(AssetConstraintSpec.ASSET))
  }

  /**
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#getType]]
   * @param id     of the EntityType
   * @param ticket implicit authentication ticket
   * @return Future Option[EntityType]
   */
  override def getType(id: Long)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    entityTypeService.getType(id, Option(AssetConstraintSpec.ASSET))
  }

  /**
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#getVersionedType]]
   * @param typeVersionId of the [[modules.core.model.TypeVersion TypeVersion]] to fetch
   * @param ticket        implicit authentication ticket
   * @return Future Option[VersionedEntityType]
   */
  override def getVersionedType(typeVersionId: Long)(implicit ticket: Ticket): Future[Option[VersionedEntityType]] = {
    entityTypeService.getVersionedType(typeVersionId, Option(AssetConstraintSpec.ASSET))
  }

  /**
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#getExtendedType]]
   * @param typeVersionId of the [[modules.core.model.EntityType EntityTypes]] TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future[ExtendedEntityType]
   */
  override def getExtendedType(typeVersionId: Long)(implicit ticket: Ticket): Future[ExtendedEntityType] = {
    entityTypeService.getExtendedType(typeVersionId, Option(AssetConstraintSpec.ASSET))
  }

  /**
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#getLatestExtendedType]]
   * @param typeId id of the parent EntityType
   * @param ticket implicit authentication ticket
   * @return Future[ExtendedEntityType]
   */
  override def getLatestExtendedType(typeId: Long)(implicit ticket: Ticket): Future[ExtendedEntityType] = {
    entityTypeService.getLatestExtendedType(typeId, Option(AssetConstraintSpec.ASSET))
  }

  /**
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#getTypeByValue]]
   * @param value  value filed (name) of the searched EntityType
   * @param ticket implicit authentication ticket
   * @return Future Option[CollectionType]
   */
  override def getTypeByValue(value: String)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    entityTypeService.getEntityTypeByValue(value, Option(AssetConstraintSpec.ASSET))
  }

  /**
   * <p> <strong>Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].</strong><br />
   *
   * Update an already existing [[modules.core.model.EntityType EntityType]]. This includes 'value' (name) and 'active'.
   * <p> Fails without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @see [[modules.core.service.ModelEntityService#updateType]]
   * @param id     of the EntityType to update
   * @param ticket implicit authentication ticket
   * @return Future[Int]
   */
  override def updateType(id: Long, value: String, active: Boolean)(implicit ticket: Ticket): Future[Int] = {
    try {
      RoleAssertion.assertModeler
      if (!AssetLogic.isStringIdentifier(value)) throw new Exception("Invalid identifier")
      typeRepository.update(EntityType(id, value, "", active))
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#getConstraintsOfType]]
   * @param typeVersionId of the parent TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future Seq[Constraint]
   */
  override def getConstraintsOfType(typeVersionId: Long)(implicit ticket: Ticket): Future[Seq[Constraint]] = {
    entityTypeService.getConstraintsOfEntityType(typeVersionId, Option(AssetConstraintSpec.ASSET))
  }

  /**
   * <p> <strong>Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].</strong><br />
   *
   * Delete a [[modules.core.model.Constraint Constraint]] by its ID.
   * <p> By deleting a Constraint, the associated [[modules.core.model.TypeVersion TypeVersion]] model must stay valid.
   * If the removal of the Constraint will invalidate the model, the future will fail.
   * <p> <strong>The removal of a 'HasProperty' Constraint leads to the system wide removal of all corresponding
   * [[modules.asset.model.Asset Asset]] [[modules.core.model.Property Properties]]!</strong>
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @see [[modules.core.service.ModelEntityService#deleteConstraint]]
   * @param id     of the Constraint to delete
   * @param ticket implicit authentication ticket
   * @return Future[Int]
   */
  override def deleteConstraint(id: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      entityTypeService.getConstraint(id) flatMap (constraintOption => {
        if (constraintOption.isEmpty) throw new Exception("No such Constraint found")
        val constraint = constraintOption.get
        getVersionedType(constraint.typeVersionId) flatMap (versionedAssetType => {
          if (versionedAssetType.isEmpty) throw new Exception("No corresponding AssetType found")
          getConstraintsOfType(versionedAssetType.get.version.id) flatMap (constraints => {

            val status = AssetLogic.isConstraintModel(constraints.filter(c => c.id != id))
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
   * <p> <strong>Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].</strong><br />
   *
   * Add a [[modules.core.model.Constraint Constraint]] to a [[modules.core.model.TypeVersion TypeVersion]].
   * <p> ID must be 0 (else the future will fail). If the addition of the Constraint will invalidate the model,
   * the future will fail.
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @see [[modules.core.service.ModelEntityService#addConstraint]]
   * @param c             String value of the [[modules.core.model.ConstraintType ConstraintType]]
   * @param v1            first Constraint parameter
   * @param v2            second Constraint parameter
   * @param typeVersionId id of the parent TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future[Long]
   */
  override def addConstraint(c: String, v1: String, v2: String, typeVersionId: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      //FIXME the ConstraintType.find() needs a check before, the rulst can be empty and lead to a unspecified exception
      val assetConstraint = Constraint(0, ConstraintType.find(c).get, v1, v2, None, typeVersionId)
      val constraintStatus = AssetLogic.isValidConstraint(assetConstraint)
      if (!constraintStatus.valid) constraintStatus.throwError
      getConstraintsOfType(assetConstraint.typeVersionId) flatMap { i =>
        val modelStatus = AssetLogic.isConstraintModel(i :+ assetConstraint)
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
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#deleteType]]
   * @param id     of the EntityType
   * @param ticket implicit authentication ticket
   * @return Future[Unit]
   */
  override def deleteType(id: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      assetRepository.deleteAssetType(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#addVersion]]
   *
   *      <p> <strong>This operation is not supported for Asset Types.</strong>
   * @param typeId of the parent EntityType
   * @param ticket implicit authentication ticket
   * @return Future[Long]
   */
  override def addVersion(typeId: Long)(implicit ticket: Ticket): Future[Long] = {
    Future.failed(new Exception("Operation not supported for Asset Types"))
  }

  /**
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].
   *
   * @see [[modules.core.service.ModelEntityService#deleteVersion]]
   *
   *      <p> <strong>This operation is not supported for Asset Types.</strong>
   * @param typeVersionId of the TypeVersion to delete
   * @param ticket        implicit authentication ticket
   * @return Future[Unit]
   */
  override def deleteVersion(typeVersionId: Long)(implicit ticket: Ticket): Future[Unit] = {
    Future.failed(new Exception("Operation not supported for Asset Types"))
  }

  /**
   * Specific implementation of to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.asset.model.Asset Assets]].<br />
   *
   * @see [[modules.core.service.ModelEntityService#forkVersion]]
   *
   *      <p> <strong>This operation is not supported for Asset Types.</strong>
   * @param typeVersionId of the TypeVersion to fork
   * @param ticket        implicit authentication ticket
   * @return Future[Unit]
   */
  override def forkVersion(typeVersionId: Long)(implicit ticket: Ticket): Future[Long] = {
    Future.failed(new Exception("Operation not supported for Asset Types"))
  }

}