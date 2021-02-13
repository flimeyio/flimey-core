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

package modules.core.service

import com.google.inject.Inject
import modules.auth.model.Ticket
import modules.auth.util.RoleAssertion
import modules.core.model.{Constraint, EntityType, ExtendedEntityType, VersionedEntityType}
import modules.core.repository.{ConstraintRepository, TypeRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EntityTypeService @Inject()(typeRepository: TypeRepository, constraintRepository: ConstraintRepository) {

  /**
   * Add a new EntityType.
   * <p> ID must be 0 and name must be unique (else the future will fail).
   * <p> Fails without MODELER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param name   the name (value) of the new EntityType
   * @param typeOf type of the entity for which this type will be created
   * @param ticket implicit authentication ticket
   * @return Future[Long]
   */
  def addType(name: String, typeOf: String)(implicit ticket: Ticket): Future[Long] = {
    try {
      RoleAssertion.assertModeler
      if (!CoreLogic.isStringIdentifier(name)) throw new Exception("Invalid identifier")
      //FIXME the input data must be validated, especially the typeOf value must match an actual type!
      typeRepository.add(EntityType(0, name, typeOf, active = false))
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all EntityTypes.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param derivesFrom optional parent type specification
   * @param ticket      implicit authentication ticket
   * @return Future Seq[EntityType]
   */
  def getAllTypes(derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[Seq[EntityType]] = {
    try {
      RoleAssertion.assertWorker
      typeRepository.getAll(derivesFrom)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all EntityTypes with their TypeVersions
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param derivesFrom optional parent type specification
   * @param ticket      implicit authentication ticket
   * @return Future Seq[VersionedEntityType]
   */
  def getAllVersions(derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[Seq[VersionedEntityType]] = {
    try {
      RoleAssertion.assertWorker
      typeRepository.getAllVersioned(derivesFrom)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all [[modules.core.model.ExtendedEntityType ExtendedEntityTypes]].
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param derivesFrom optional parent type specification
   * @param ticket      implicit authentication ticket
   * @return Future Seq[ExtendedEntityType]
   */
  def getAllExtendedTypes(derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[Seq[ExtendedEntityType]] = {
    try {
      RoleAssertion.assertWorker
      typeRepository.getAllExtended(derivesFrom)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get an EntityType by its ID.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id          od the AssetType
   * @param derivesFrom optional parent type specification
   * @param ticket      implicit authentication ticket
   * @return Future Option[EntityType]
   */
  def getType(id: Long, derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    try {
      RoleAssertion.assertWorker
      typeRepository.get(id, derivesFrom)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get an EntityType by its ID.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param typeVersionId od the TypeVersion
   * @param derivesFrom   optional parent type specification
   * @param ticket        implicit authentication ticket
   * @return Future Option[VersionedEntityType]
   */
  def getVersionedType(typeVersionId: Long, derivesFrom: Option[String] = None)(implicit ticket: Ticket):
  Future[Option[VersionedEntityType]] = {
    try {
      RoleAssertion.assertWorker
      typeRepository.getVersioned(typeVersionId, derivesFrom)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get an ExtendedEntityType by its TypeVersion id.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param typeVersionId id of the TypeVersion of the EntityType
   * @param derivesFrom   optional parent type specification
   * @param ticket        implicit authentication ticket
   * @return Future[ExtendedEntityType]
   */
  def getExtendedType(typeVersionId: Long, derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[ExtendedEntityType] = {
    try {
      RoleAssertion.assertWorker
      typeRepository.getExtended(typeVersionId, derivesFrom) map (typeData => {
        if (typeData.isEmpty) throw new Exception("Invalid entity type")
        typeData.get
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get the latest version of an ExtendedEntityType by its typeId.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param typeId      id of the of the EntityType
   * @param derivesFrom optional parent type specification
   * @param ticket      implicit authentication ticket
   * @return Future[ExtendedEntityType]
   */
  def getLatestExtendedType(typeId: Long, derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[ExtendedEntityType] = {
    try {
      RoleAssertion.assertWorker
      typeRepository.getAllExtendedVersions(typeId, derivesFrom).map(_.maxBy(_.version.version))
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }


  /**
   * Get an EntityType by its value (name) field.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   * //TODO this can be extended to provide substring search results.
   *
   * @param value       value filed (name) of the searched EntityType
   * @param derivesFrom optional parent type specification
   * @param ticket      implicit authentication ticket
   * @return Future Option[EntityType]
   */
  def getEntityTypeByValue(value: String, derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    try {
      RoleAssertion.assertWorker
      //FIXME this is not critical because there won't be many AssetTypes but filtering should be done in the repository.
      getAllTypes(derivesFrom) flatMap (types => Future.successful(types.find(_.value == value)))
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all Constraints associated to a TypeVersion.
   * <p> Fails without WORKER rights.
   * <p>This is a safe implementation and can be used by controller classes.
   *
   * @param typeVersionId of the TypeVersion of the EntityType
   * @param derivesFrom   optional parent type specification
   * @param ticket        implicit authentication ticket
   * @return Future Seq[Constraint]
   */
  def getConstraintsOfEntityType(typeVersionId: Long, derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[Seq[Constraint]] = {
    try {
      RoleAssertion.assertWorker
      constraintRepository.getAssociated(typeVersionId, derivesFrom)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get an Constraint by its ID.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the Constraint
   * @param ticket implicit authentication ticket
   * @return Future Option[Constraint]
   */
  def getConstraint(id: Long)(implicit ticket: Ticket): Future[Option[Constraint]] = {
    try {
      RoleAssertion.assertWorker
      constraintRepository.get(id)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

}
