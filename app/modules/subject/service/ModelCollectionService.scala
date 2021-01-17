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
import modules.core.model.{Constraint, ConstraintType, EntityType}
import modules.core.repository.{ConstraintRepository, TypeRepository}
import modules.core.service.{EntityTypeService, ModelEntityService}
import modules.subject.model.CollectionConstraintSpec
import modules.subject.repository.CollectionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * FIXME
 *
 * @param typeRepository
 * @param constraintRepository
 * @param collectionRepository
 * @param entityTypeService
 */
class ModelCollectionService @Inject()(typeRepository: TypeRepository,
                                       constraintRepository: ConstraintRepository,
                                       collectionRepository: CollectionRepository,
                                       entityTypeService: EntityTypeService) extends ModelEntityService {

  /**
   * Get all CollectionTypes.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param ticket implicit authentication ticket
   * @return Future Seq[EntityType]
   */
  override def getAllTypes()(implicit ticket: Ticket): Future[Seq[EntityType]] = {
    entityTypeService.getAllTypes(Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * Get an CollectionType by its ID.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     idd the CollectionType
   * @param ticket implicit authentication ticket
   * @return Future Option[EntityType]
   */
  override def getType(id: Long)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    entityTypeService.getType(id, Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * Get a complete CollectionType (Head + Constraints).
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     od the CollectionType
   * @param ticket implicit authentication ticket
   * @return Future (EntityType, Seq[Constraint])
   */
  override def getCompleteType(id: Long)(implicit ticket: Ticket): Future[(EntityType, Seq[Constraint])] = {
    entityTypeService.getCompleteType(id, Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * Get an CollectionType by its value (name) field.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param value  value filed (name) of the searched CollectionType
   * @param ticket implicit authentication ticket
   * @return Future Option[CollectionType]
   */
  override def getTypeByValue(value: String)(implicit ticket: Ticket): Future[Option[EntityType]] = {
    entityTypeService.getEntityTypeByValue(value, Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * Update an already existing CollectionType entity. This includes 'value' (name) and 'active'.
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
      if (!CollectionLogic.isStringIdentifier(value)) throw new Exception("Invalid identifier")
      if (active) {
        getConstraintsOfType(id) flatMap (constraints => {
          val status = CollectionLogic.isConstraintModel(constraints)
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
   * Get all Constraints associated to an CollectionType.
   * <p> Fails without WORKER rights.
   * <p>This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the CollectionType
   * @param ticket implicit authentication ticket
   * @return Future Seq[Constraint]
   */
  override def getConstraintsOfType(id: Long)(implicit ticket: Ticket): Future[Seq[Constraint]] = {
    entityTypeService.getConstraintsOfEntityType(id, Option(CollectionConstraintSpec.COLLECTION))
  }

  /**
   * Delete a CollectionConstraint by its ID.
   * <p> By deleting a Constraint, the associated ACollectionType model must stay valid.
   * If the removal of the Constraint will invalidate the model, the future will fail.
   * <p> <strong>The removal of a 'HasProperty' Constraint leads to the system wide removal of all corresponding
   * Collection data properties!</strong>
   * //FIXME what happens if uses plugin or can contain is removed -> in this case also strict deletion!
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
        getType(constraint.typeId) flatMap (collectionType => {
          if (collectionType.isEmpty) throw new Exception("No corresponding EntityType found")

          getConstraintsOfType(collectionType.get.id) flatMap (constraints => {

            val deletedConstraints = CollectionLogic.removeConstraint(constraint, constraints)

            val status = CollectionLogic.isConstraintModel(constraints.filter(c => c.id != id))
            if (!status.valid) status.throwError

            if (constraint.c == ConstraintType.HasProperty) {
              collectionRepository.deletePropertyConstraint(constraint)
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
   * Add a Constraint to a CollectionType.
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
  override def addConstraint(c: String, v1: String, v2: String, typeId: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertModeler
      //FIXME the ConstraintType.find() needs a check before, the rules can be empty and lead to a unspecified exception
      val newConstraint = Constraint(0, ConstraintType.find(c).get, v1, v2, None, typeId)
      //val constraintStatus = AssetLogic.isValidConstraint(assetConstraint)
      //if (!constraintStatus.valid) constraintStatus.throwError
      getConstraintsOfType(newConstraint.typeId) flatMap { constraints =>

        val newConstraints = CollectionLogic.applyConstraint(newConstraint)

        //val modelStatus = AssetLogic.isConstraintModel(i :+ assetConstraint)
        //if (!modelStatus.valid) modelStatus.throwError

        //if (assetConstraint.c == ConstraintType.HasProperty) {
        //  assetRepository.addPropertyConstraint(assetConstraint)
        //} else {
        constraintRepository.addConstraint(newConstraint) map (_ -> Future.unit)
        //}
      }
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete a CollectionType.
   * <p> <strong> This operation will also delete all associated Constraints and all Collections which have this type! </strong>
   * <p> Fails without MODELER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param id     of the CollectionType
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

}
