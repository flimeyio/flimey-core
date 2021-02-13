/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2021 Karl Kegel
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

package modules.subject.service

import com.google.inject.Inject
import modules.auth.model.Ticket
import modules.auth.util.RoleAssertion
import modules.core.model.{Constraint, ConstraintType, EntityType, ExtendedEntityType, VersionedEntityType}
import modules.core.repository.{ConstraintRepository, TypeRepository, ViewerRepository}
import modules.core.service.{EntityTypeService, ModelEntityService}
import modules.subject.model.CollectibleConstraintSpec
import modules.subject.repository.CollectibleRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The service class to provide safe functionality to work with [[modules.core.model.EntityType EntityTypes]] of
 * [[modules.subject.model.Collectible Collectibles]].
 * <p> Normally, this class is used with dependency injection in controller classes or as helper in other services.
 *
 * @param typeRepository       injected [[modules.core.repository.TypeRepository TypeRepository]]
 * @param constraintRepository injected [[modules.core.repository.ConstraintRepository ConstraintRepository]]
 * @param collectibleRepository
 * @param viewerRepository
 * @param entityTypeService    injected [[modules.core.service.EntityTypeService EntityTypeService]]
 */
class ModelCollectibleService @Inject()(typeRepository: TypeRepository,
                                        constraintRepository: ConstraintRepository,
                                        collectibleRepository: CollectibleRepository,
                                        viewerRepository: ViewerRepository,
                                        entityTypeService: EntityTypeService) extends ModelEntityService {

  /**
   * Get all [[modules.subject.model.Collectible Collectible]] [[modules.core.model.EntityType EntityTypes]].
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param ticket implicit authentication ticket
   * @return Future Seq[EntityType]
   */
  override def getAllTypes()(implicit ticket: Ticket): Future[Seq[EntityType]] = {
    entityTypeService.getAllTypes(Some(CollectibleConstraintSpec.COLLECTIBLE))
  }

  override def getAllVersions()(implicit ticket: Ticket): Future[Seq[VersionedEntityType]] = {
    entityTypeService.getAllVersions(Some(CollectibleConstraintSpec.COLLECTIBLE))
  }

  override def addVersion(typeId: Long)(implicit ticket: Ticket): Future[Long] = {
    Future.failed(new Exception("Not implemented yet"))
  }

  override def deleteVersion(typeVersionId: Long)(implicit ticket: Ticket): Future[Unit] = {
    Future.failed(new Exception("Not implemented yet"))
  }

  /**
   * Get all [[modules.core.model.ExtendedEntityType ExtendedEntityTypes]] which define
   * [[modules.subject.model.Collectible Collectibles]].
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param ticket implicit authentication ticket
   * @return Future Seq[EntityType]
   */
  def getAllExtendedTypes()(implicit ticket: Ticket): Future[Seq[ExtendedEntityType]] = {
    entityTypeService.getAllExtendedTypes(Some(CollectibleConstraintSpec.COLLECTIBLE))
  }

  /**
   * Get an [[modules.subject.model.Collectible Collectible]] [[modules.core.model.EntityType EntityType]] by its ID.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     idd the Collectible Type
   * @param ticket implicit authentication ticket
   * @return Future Option[EntityType]
   */
  override def getType(id: Long)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    entityTypeService.getType(id, Some(CollectibleConstraintSpec.COLLECTIBLE))
  }

  override def getVersionedType(typeVersionId: Long)(implicit ticket: Ticket): Future[Option[VersionedEntityType]] = {
    entityTypeService.getVersionedType(typeVersionId, Some(CollectibleConstraintSpec.COLLECTIBLE))
  }

  /**
   * Get a complete [[modules.subject.model.Collectible Collectible]] [[modules.core.model.EntityType EntityType]] (Head + Constraints).
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param typeVersionId     od the TypeVersion
   * @param ticket implicit authentication ticket
   * @return Future (EntityType, Seq[Constraint])
   */
  override def getExtendedType(typeVersionId: Long)(implicit ticket: Ticket): Future[ExtendedEntityType] = {
    entityTypeService.getExtendedType(typeVersionId, Some(CollectibleConstraintSpec.COLLECTIBLE))
  }

  override def getLatestExtendedType(typeId: Long)(implicit ticket: Ticket): Future[ExtendedEntityType] = {
    entityTypeService.getLatestExtendedType(typeId, Some(CollectibleConstraintSpec.COLLECTIBLE))
  }

  /**
   * Get an [[modules.subject.model.Collectible Collectible]] [[modules.core.model.EntityType EntityType]] by its value (name) field.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param value  value filed (name) of the searched Collectible Type
   * @param ticket implicit authentication ticket
   * @return Future Option[EntityType]
   */
  override def getTypeByValue(value: String)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    entityTypeService.getEntityTypeByValue(value, Some(CollectibleConstraintSpec.COLLECTIBLE))
  }

  /**
   * Update an already existing [[modules.core.model.EntityType EntityType]] entity. This includes 'value' (name) and 'active'.
   * <p> To change the 'active' property to true, the Constraint model must be valid!
   * <p> Fails without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the Type to update
   * @param ticket implicit authentication ticket
   * @return Future[Int]
   */
  override def updateType(id: Long, value: String, active: Boolean)(implicit ticket: Ticket): Future[Int] = {
    try {
      RoleAssertion.assertModeler
      if (!CollectibleLogic.isStringIdentifier(value)) throw new Exception("Invalid identifier")
      if (active) {
        getConstraintsOfType(id) flatMap (constraints => {
          val status = CollectibleLogic.isConstraintModel(constraints)
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
   * Get all Constraints associated to an [[modules.subject.model.Collectible Collectible]] [[modules.core.model.EntityType EntityType]].
   * <p> Fails without WORKER rights.
   * <p>This is a safe implementation and can be used by controller classes.
   *
   * @param typeVersionId     of the TypeVersion
   * @param ticket implicit authentication ticket
   * @return Future Seq[Constraint]
   */
  override def getConstraintsOfType(typeVersionId: Long)(implicit ticket: Ticket): Future[Seq[Constraint]] = {
    entityTypeService.getConstraintsOfEntityType(typeVersionId, Some(CollectibleConstraintSpec.COLLECTIBLE))
  }

  /**
   * Delete a [[modules.subject.model.Collectible Collectible]] [[modules.core.model.Constraint Constraint]] by its ID.
   * <p> By deleting a Constraint, the associated [[modules.core.model.EntityType EntityType]] model must stay valid.
   * If the removal of the Constraint will invalidate the model, the future will fail.
   * <p> <strong>The removal of a HasProperty or UsesPlugin Constraint leads to the system wide removal of all corresponding
   * Collectible data properties!</strong>
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
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

        getVersionedType(constraint.typeVersionId) flatMap (collectibleType => {
          if (collectibleType.isEmpty) throw new Exception("No corresponding EntityType found")

          getConstraintsOfType(constraint.typeVersionId) flatMap (constraints => {
            val deletedConstraints = CollectibleLogic.removeConstraint(constraint, constraints)
            val remainingConstraints = constraints.filter(c => !deletedConstraints.contains(c))

            val status = CollectibleLogic.isConstraintModel(remainingConstraints)
            if (!status.valid) status.throwError

            val deletedPropertyConstraints = deletedConstraints.filter(_.c == ConstraintType.HasProperty)
            val deletedOtherConstraints = deletedConstraints diff deletedPropertyConstraints

            collectibleRepository.deleteConstraints(constraint.typeVersionId, deletedPropertyConstraints, deletedOtherConstraints)
          })
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Add a [[modules.core.model.Constraint Constraint]] to a [[modules.subject.model.Collectible Collectible]]
   * [[modules.core.model.EntityType EntityType]].
   * <p> If adding the Constraint will invalidate the model, the future will fail.
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param c      String value of the ConstraintType
   * @param v1     first Constraint parameter
   * @param v2     second Constraint parameter
   * @param typeVersionId id of the parent TypeVersion
   * @param ticket implicit authentication ticket
   * @return Future[Long]
   */
  override def addConstraint(c: String, v1: String, v2: String, typeVersionId: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      //FIXME the ConstraintType.find() needs a check before, the rules can be empty and lead to a unspecified exception
      val newConstraint = Constraint(0, ConstraintType.find(c).get, v1, v2, None, typeVersionId)
      val constraintStatus = CollectibleLogic.isValidConstraint(newConstraint)
      if (!constraintStatus.valid) constraintStatus.throwError

      getVersionedType(newConstraint.typeVersionId) flatMap (collectibleType => {
        if (collectibleType.isEmpty) throw new Exception("No corresponding EntityType found")

        getConstraintsOfType(newConstraint.typeVersionId) flatMap (constraints => {

          val newConstraints = CollectibleLogic.applyConstraint(newConstraint)
          val newConstraintModel = constraints ++ newConstraints

          val modelStatus = CollectibleLogic.isConstraintModel(newConstraintModel)
          if (!modelStatus.valid) modelStatus.throwError

          val newPropertyConstraints = newConstraints.filter(_.c == ConstraintType.HasProperty)
          val newOtherConstraints = newConstraints diff newPropertyConstraints

          collectibleRepository.addConstraints(typeVersionId, newPropertyConstraints, newOtherConstraints)
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete a [[modules.subject.model.Collectible Collectible]] [[modules.core.model.EntityType EntityType]].
   * <p> <strong> This operation will also delete all associated Constraints and all Collectibles which have this type! </strong>
   * <p> Fails without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the Collectible Type
   * @param ticket implicit authentication ticket
   * @return Future[Unit]
   */
  override def deleteType(id: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      collectibleRepository.deleteCollectibleType(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

}
