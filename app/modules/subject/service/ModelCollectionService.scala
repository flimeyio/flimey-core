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
import modules.core.model._
import modules.core.repository.{ConstraintRepository, TypeRepository}
import modules.core.service.{EntityTypeService, ModelEntityService}
import modules.subject.model.CollectionConstraintSpec
import modules.subject.repository.CollectionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The service class to provide safe functionality to work with [[modules.core.model.EntityType EntityTypes]] of
 * [[modules.subject.model.Collection Collections]].
 * <p> Normally, this class is used with dependency injection in controller classes or as helper in other services.
 *
 * @param typeRepository       injected [[modules.core.repository.TypeRepository TypeRepository]]
 * @param constraintRepository injected [[modules.core.repository.ConstraintRepository ConstraintRepository]]
 * @param collectionRepository injected [[modules.subject.repository.CollectionRepository]]
 * @param entityTypeService    injected [[modules.core.service.EntityTypeService EntityTypeService]]
 */
class ModelCollectionService @Inject()(typeRepository: TypeRepository,
                                       constraintRepository: ConstraintRepository,
                                       collectionRepository: CollectionRepository,
                                       entityTypeService: EntityTypeService) extends ModelEntityService {

  /**
   * Get all [[modules.core.model.EntityType EntityTypes]] which describe [[modules.subject.model.Collection Collections]].
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#getAllTypes]]
   * @param ticket implicit authentication ticket
   * @return Future Seq[EntityType]
   */
  override def getAllTypes()(implicit ticket: Ticket): Future[Seq[EntityType]] = {
    entityTypeService.getAllTypes(Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#getAllVersions]]
   * @param ticket implicit authentication ticket
   * @return Future Seq[VersionedEntityType]
   */
  override def getAllVersions()(implicit ticket: Ticket): Future[Seq[VersionedEntityType]] = {
    entityTypeService.getAllVersions(Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#addVersion]]
   * @param typeId of the parent EntityType
   * @param ticket implicit authentication ticket
   * @return Future[Long]
   */
  override def addVersion(typeId: Long)(implicit ticket: Ticket): Future[Long] = {
    entityTypeService.addVersion(typeId)
  }

  /**
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#deleteVersion]]
   * @param typeVersionId of the TypeVersion to delete
   * @param ticket        implicit authentication ticket
   * @return Future[Unit]
   */
  override def deleteVersion(typeVersionId: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      getVersionedType(typeVersionId) flatMap (versionedTypeOption => {
        if (versionedTypeOption.isEmpty) throw new Exception("No such version found")
        val versionedType = versionedTypeOption.get
        typeRepository.getAllExtendedVersions(versionedType.entityType.id, Some(CollectionConstraintSpec.COLLECTION)) flatMap (allVersions => {
          if (allVersions.size == 1) throw new Exception("Every type must have at least one version")
          collectionRepository.deleteCollectionTypeVersion(typeVersionId)
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#forkVersion]]
   * @param typeVersionId of the TypeVersion to fork
   * @param ticket        implicit authentication ticket
   * @return Future[Unit]
   */
  override def forkVersion(typeVersionId: Long)(implicit ticket: Ticket): Future[Long] = {
    entityTypeService.forkVersion(typeVersionId)
  }

  /**
   * Get all [[modules.core.model.ExtendedEntityType ExtendedEntityTypes]] which define
   * [[modules.subject.model.Collection Collections]].
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param ticket implicit authentication ticket
   * @return Future Seq[EntityType]
   */
  def getAllExtendedTypes()(implicit ticket: Ticket): Future[Seq[ExtendedEntityType]] = {
    entityTypeService.getAllExtendedTypes(Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * Get an Collection [[modules.core.model.EntityType EntityType]] by its ID.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#getType]]
   * @param id     id the EntityType
   * @param ticket implicit authentication ticket
   * @return Future Option[EntityType]
   */
  override def getType(id: Long)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    entityTypeService.getType(id, Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#getVersionedType]]
   * @param typeVersionId of the [[modules.core.model.TypeVersion TypeVersion]] to fetch
   * @param ticket        implicit authentication ticket
   * @return Future Option[VersionedEntityType]
   */
  override def getVersionedType(typeVersionId: Long)(implicit ticket: Ticket): Future[Option[VersionedEntityType]] = {
    entityTypeService.getVersionedType(typeVersionId, Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * Get a complete [[modules.core.model.ExtendedEntityType ExtendedEntityType]] of a specified
   * [[modules.core.model.TypeVersion TypeVersion]]
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#getExtendedType]]
   * @param typeVersionId id of the TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future (EntityType, Seq[Constraint])
   */
  override def getExtendedType(typeVersionId: Long)(implicit ticket: Ticket): Future[ExtendedEntityType] = {
    entityTypeService.getExtendedType(typeVersionId, Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#getLatestExtendedType]]
   * @param typeId id of the parent EntityType
   * @param ticket implicit authentication ticket
   * @return Future[ExtendedEntityType]
   */
  override def getLatestExtendedType(typeId: Long)(implicit ticket: Ticket): Future[ExtendedEntityType] = {
    entityTypeService.getLatestExtendedType(typeId, Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * Get an [[modules.core.model.EntityType EntityType]] by its value (name) field.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#getTypebyValue]]
   * @param value  value filed (name) of the searched EntityType
   * @param ticket implicit authentication ticket
   * @return Future Option[CollectionType]
   */
  override def getTypeByValue(value: String)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    entityTypeService.getEntityTypeByValue(value, Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * Update an already existing [[modules.core.model.EntityType EntityType]] entity. This includes 'value' (name) and 'active'.
   * <p> Fails without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#updateType]]
   * @param id     of the EntityType to update
   * @param ticket implicit authentication ticket
   * @return Future[Int]
   */
  override def updateType(id: Long, value: String, active: Boolean)(implicit ticket: Ticket): Future[Int] = {
    try {
      RoleAssertion.assertModeler
      if (!CollectionLogic.isStringIdentifier(value)) throw new Exception("Invalid identifier")
      typeRepository.update(EntityType(id, value, "", active))
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all Constraints associated to an [[modules.core.model.TypeVersion TypeVersion]].
   * <p> Fails without WORKER rights.
   * <p>This is a safe implementation and can be used by controller classes.
   *
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#getConstraintsOfType]]
   * @param typeVersionId of the TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future Seq[Constraint]
   */
  override def getConstraintsOfType(typeVersionId: Long)(implicit ticket: Ticket): Future[Seq[Constraint]] = {
    entityTypeService.getConstraintsOfEntityType(typeVersionId, Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * Delete a [[modules.core.model.Constraint Constraint]] by its ID.
   * <p> By deleting a Constraint, the associated [[modules.core.model.TypeVersion TypeVersion]] model must stay valid.
   * If the removal of the Constraint will invalidate the model, the future will fail.
   * <p> <strong>The removal of a HasProperty or UsesPlugin Constraint leads to the system wide removal of all corresponding
   * Collection data properties of this TypeVersion!</strong>
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
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

        getVersionedType(constraint.typeVersionId) flatMap (collectionType => {
          if (collectionType.isEmpty) throw new Exception("No corresponding EntityType found")
          val typeVersionId = collectionType.get.version.id

          getConstraintsOfType(typeVersionId) flatMap (constraints => {
            val deletedConstraints = CollectionLogic.removeConstraint(constraint, constraints)
            val remainingConstraints = constraints.filter(c => !deletedConstraints.contains(c))

            val status = CollectionLogic.isConstraintModel(remainingConstraints)
            if (!status.valid) status.throwError

            val deletedPropertyConstraints = deletedConstraints.filter(_.c == ConstraintType.HasProperty)
            val deletedOtherConstraints = deletedConstraints diff deletedPropertyConstraints

            collectionRepository.deleteConstraints(typeVersionId, deletedPropertyConstraints, deletedOtherConstraints)
          })
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Add a [[modules.core.model.Constraint Constraint]] to a [[modules.core.model.TypeVersion TypeVersion]].
   * <p> ID must be 0 (else the future will fail). If the addition of the Constraint will invalidate the model,
   * the future will fail.
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#addConstraint]]
   * @param c             String value of the ConstraintType
   * @param v1            first Constraint parameter
   * @param v2            second Constraint parameter
   * @param typeVersionId id of the parent TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future[Long]
   */
  override def addConstraint(c: String, v1: String, v2: String, typeVersionId: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      //FIXME the ConstraintType.find() needs a check before, the rules can be empty and lead to a unspecified exception
      val newConstraint = Constraint(0, ConstraintType.find(c).get, v1, v2, None, typeVersionId)
      val constraintStatus = CollectionLogic.isValidConstraint(newConstraint)
      if (!constraintStatus.valid) constraintStatus.throwError

      getVersionedType(newConstraint.typeVersionId) flatMap (collectionType => {
        if (collectionType.isEmpty) throw new Exception("No corresponding EntityType found")

        getConstraintsOfType(newConstraint.typeVersionId) flatMap (constraints => {

          val newConstraints = CollectionLogic.applyConstraint(newConstraint)
          val newConstraintModel = constraints ++ newConstraints

          val modelStatus = CollectionLogic.isConstraintModel(newConstraintModel)
          if (!modelStatus.valid) modelStatus.throwError

          val newPropertyConstraints = newConstraints.filter(_.c == ConstraintType.HasProperty)
          val newOtherConstraints = newConstraints diff newPropertyConstraints

          collectionRepository.addConstraints(typeVersionId, newPropertyConstraints, newOtherConstraints)
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete a [[modules.core.model.EntityType EntityType]] of a Collection.
   * <p> <strong> This operation will also delete all associated Constraints, TypeVersions and all Collections which have this type! </strong>
   * <p> Fails without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * <p><strong> Specific implementation to work only with [[modules.core.model.EntityType EntityTypes]] which specify
   * [[modules.subject.model.Collection Collections]].</strong>
   *
   * @see [[modules.core.service.ModelEntityService#deleteType]]
   * @param id     of the EntityType
   * @param ticket implicit authentication ticket
   * @return Future[Unit]
   */
  override def deleteType(id: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      collectionRepository.deleteCollectionType(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all possible children of a [[modules.subject.model.Collection Collection]].
   * <p> Only the by CanContain [[modules.core.model.Constraint Constraints]] defined string values are used
   * without checking, if such a [[modules.core.model.EntityType EntityType]] exists actually.
   * <p> Fails without WORKER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param typeVersionId id of the paren TypeVersion
   * @param ticket        implicit authentication ticket
   * @return Future Seq[EntityType]
   */
  def getChildren(typeVersionId: Long)(implicit ticket: Ticket): Future[Seq[ExtendedEntityType]] = {
    try {
      RoleAssertion.assertWorker
      typeRepository.getExtended(typeVersionId, Some(CollectionConstraintSpec.COLLECTION)) flatMap (typeData => {
        val childValues = CollectionLogic.findChildren(typeData.get.constraints)
        typeRepository.getAllExtended(childValues).map(values => values.groupBy(_.entityType).mapValues(extendedTypes => extendedTypes.maxBy(_.version.version)).values toSeq)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

}
