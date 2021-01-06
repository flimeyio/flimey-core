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
import modules.core.model.{Constraint, EntityType}
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
   * Get a complete EntityType (Head + Constraints).
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id          id of the EntityType
   * @param derivesFrom optional parent type specification
   * @param ticket      implicit authentication ticket
   * @return Future (EntityType, Seq[Constraint])
   */
  def getCompleteType(id: Long, derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[(EntityType, Seq[Constraint])] = {
    try {
      RoleAssertion.assertWorker
      typeRepository.getComplete(id, derivesFrom) map (typeData => {
        val (entityType, constraints) = typeData
        if (entityType.isEmpty) throw new Exception("Invalid asset type")
        (entityType.get, constraints)
      })
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
   * Get all Constraints associated to an EntityType.
   * <p> Fails without WORKER rights.
   * <p>This is a safe implementation and can be used by controller classes.
   *
   * @param id          of the EntityType
   * @param derivesFrom optional parent type specification
   * @param ticket      implicit authentication ticket
   * @return Future Seq[Constraint]
   */
  def getConstraintsOfEntityType(id: Long, derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[Seq[Constraint]] = {
    try {
      RoleAssertion.assertWorker
      constraintRepository.getAssociated(id, derivesFrom)
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

  /**
   * Get all EntityTypes, a specific EntityType and its Constraints at once.
   * <p> This operation is just a future comprehension of different service methods.
   * <p> Fails without WORKER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of an EntityType
   * @param ticket implicit authentication ticket
   * @return Future Tuple of all EntityTypes, a specific EntityType and its Constraints
   */
  def getCombinedEntity(id: Long, derivesFrom: Option[String] = None)(implicit ticket: Ticket): Future[(Seq[EntityType], Option[EntityType], Seq[Constraint])] = {
    try {
      RoleAssertion.assertWorker
      (for {
        entityTypes <- getAllTypes(derivesFrom)
        constraints <- getConstraintsOfEntityType(id, derivesFrom)
      } yield (entityTypes, constraints)) map (res => {
        (res._1, res._1.find(p => p.id == id), res._2)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }
}
