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

import java.sql.Timestamp
import java.time.Instant

import com.google.inject.Inject
import modules.auth.model.Ticket
import modules.auth.util.RoleAssertion
import modules.core.model.Constraint
import modules.core.repository.FlimeyEntityRepository
import modules.subject.model._
import modules.subject.repository.CollectibleRepository
import modules.user.util.ViewerAssertion

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The service class to provide safe functionality to work with [[modules.subject.model.Collectible Collectibles]].
 * <p> Normally, this class is used with dependency injection in controller classes or as helper in other services.
 *
 * @param collectibleRepository   injected [[modules.subject.repository.CollectibleRepository CollectibleRepository]]
 * @param entityRepository        injected [[modules.core.repository.FlimeyEntityRepository FlimeyEntityRepository]]
 * @param collectionService       injected [[modules.subject.service.CollectionService CollectionService]]
 * @param modelCollectibleService injected [[modules.subject.service.ModelCollectibleService ModelCollectibleService]]
 * @param modelCollectionService  injected [[modules.subject.service.ModelCollectionService ModelCollectionService]]
 */
class CollectibleService @Inject()(collectibleRepository: CollectibleRepository,
                                   collectionService: CollectionService,
                                   modelCollectibleService: ModelCollectibleService,
                                   modelCollectionService: ModelCollectionService,
                                   entityRepository: FlimeyEntityRepository) {

  /**
   * Add a new [[modules.subject.model.Collectible Collectible]].
   * <p> To create the Collectible, the newest available [[modules.core.model.TypeVersion TypeVersion]] of the selected
   * [[modules.core.model.EntityType EntityType]].
   * <p> Fails without WORKER rights.
   * <p> Requires EDITOR rights in the parent Collection.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param collectionId id of the parent [[modules.subject.model.Collection Collection]]
   * @param typeId       id of the Collectible [[modules.core.model.EntityType]]
   * @param propertyData of the new Collectible (must complete the Collectible TypeVersion model)
   * @param ticket       implicit authentication ticket
   * @return Future[Unit]
   */
  def addCollectible(collectionId: Long, typeId: Long, propertyData: Seq[String])(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertWorker
      collectionService.getSlimCollection(collectionId) flatMap (collectionHeader => {
        ViewerAssertion.assertEdit(collectionHeader.viewers)
        for {
          collectionTypeData <- modelCollectionService.getExtendedType(collectionHeader.collection.typeVersionId)
          collectibleTypeData <- modelCollectibleService.getLatestExtendedType(typeId)
        } yield {
          if (!collectionTypeData.entityType.active) throw new Exception("The selected Collection Type is not defined or active")
          if (!collectibleTypeData.entityType.active) throw new Exception("The selected Collectible Type is not defined or active")
          val extensionStatus = CollectibleLogic.canBeChildOf(collectibleTypeData.entityType.value, collectionTypeData.constraints)
          if (!extensionStatus.valid) extensionStatus.throwError

          val properties = CollectibleLogic.derivePropertiesFromRawData(collectibleTypeData.constraints, propertyData)
          val configurationStatus = CollectibleLogic.isModelConfiguration(collectibleTypeData.constraints, properties)
          if (!configurationStatus.valid) configurationStatus.throwError

          collectibleRepository.add(
            Collectible(0, 0, collectionHeader.collection.id, collectibleTypeData.version.id, SubjectState.CREATED, Timestamp.from(Instant.now())), properties)

        }
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Update a [[modules.subject.model.Collectible Collectible]] with its [[modules.core.model.Property Properties]].
   * <p> All Properties (if updated or not) must be passed, else the configuration can not be verified.
   * <p> Fails without WORKER rights.
   * <p> If Properties are changed, EDITOR rights are required.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param collectibleId      id of the Collectible to update
   * @param propertyUpdateData all Properties of the changed Collectible (can contain updated values)
   * @param ticket             implicit authentication ticket
   * @return Future[Unit]
   */
  def updateCollectible(collectibleId: Long, propertyUpdateData: Seq[String])(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertWorker
      getCollectible(collectibleId) flatMap (extendedCollectible => {

        //Check if the User can edit this Collectible
        ViewerAssertion.assertEdit(extendedCollectible.viewers)

        //Parse updated properties and verify the configuration
        val properties = extendedCollectible.properties
        val oldConfig = properties
        val newConfig = CollectibleLogic.mapConfigurations(oldConfig, propertyUpdateData)

        //check if the EntityType of the Collectible is active (else it can not be edited)
        modelCollectibleService.getExtendedType(extendedCollectible.collectible.typeVersionId) flatMap (extendedEntityType => {
          if (!extendedEntityType.entityType.active) throw new Exception("The selected Collectible Type is not active")
          val configurationStatus = CollectibleLogic.isModelConfiguration(extendedEntityType.constraints, newConfig)
          if (!configurationStatus.valid) configurationStatus.throwError

          entityRepository.update(newConfig, Set(), Set())
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Update the [[modules.subject.model.SubjectState State]] of a [[modules.subject.model.Collectible Collectible]].
   * <p> Fails without WORKER rights.
   * <p> The requesting User must be EDITOR.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param collectibleId id of the Collectible to update
   * @param newState      state string value
   * @param ticket        implicit authentication ticket
   * @return Future[Unit]
   */
  def updateState(collectibleId: Long, newState: String)(implicit ticket: Ticket): Future[Int] = {
    try {
      RoleAssertion.assertWorker
      getCollectible(collectibleId) flatMap (extendedCollectible => {
        //Check if the User can edit this Collectible
        ViewerAssertion.assertEdit(extendedCollectible.viewers)
        val state = CollectionLogic.parseState(newState);
        val updateStatus = CollectibleLogic.isValidStateTransition(extendedCollectible.collectible.state, state)
        if (!updateStatus.valid) updateStatus.throwError
        collectibleRepository.updateState(collectibleId, state)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get a [[modules.subject.model.ExtendedCollectible ExtendedCollectible]] together with its
   * [[modules.core.model.Property Properties]] and [[modules.core.model.Viewer Viewers]] by id.
   * <p> A User (given by his ticket) can only request Collectibles he has access rights to.
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param collectibleId id of the Collectible to fetch
   * @param ticket        implicit authentication ticket
   * @return Future[ExtendedCollectible]
   */
  def getCollectible(collectibleId: Long)(implicit ticket: Ticket): Future[ExtendedCollectible] = {
    try {
      RoleAssertion.assertWorker
      val accessedGroupIds = ticket.accessRights.getAllViewingGroupIds
      collectibleRepository.getExtendedCollectible(collectibleId) map (extendedCollectibleOption => {
        if (extendedCollectibleOption.isEmpty) throw new Exception("No such a Collectible found")
        val extendedCollectible = extendedCollectibleOption.get
        ViewerAssertion.assertView(extendedCollectible.viewers)
        extendedCollectible
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete a [[modules.subject.model.Collectible Collectible]].
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without MAINTAINER rights
   *
   * @param id     of the Collectible
   * @param ticket implicit authentication ticket
   * @return Future[Unit]
   */
  def deleteCollectible(id: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertWorker
      getCollectible(id) flatMap (extendedCollectible => {
        ViewerAssertion.assertMaintain(extendedCollectible.viewers)
        collectibleRepository.delete(extendedCollectible.collectible)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Forwards to same method of [[modules.subject.service.CollectibleLogic CollectibleLogic]].
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without WORKER rights
   *
   * @param constraints model of a [[modules.core.model.TypeVersion TypeVersion]]
   * @param ticket      implicit authentication ticket
   * @return Seq[(String, String)] (property key -> data type)
   */
  def getCollectiblePropertyKeys(constraints: Seq[Constraint])(implicit ticket: Ticket): Seq[(String, String)] = {
    RoleAssertion.assertWorker
    CollectibleLogic.getPropertyKeys(constraints)
  }

  /**
   * Forwards to same method of [[modules.subject.service.CollectibleLogic CollectibleLogic]].
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without WORKER rights
   *
   * @param constraints model of a [[modules.core.model.TypeVersion TypeVersion]]
   * @param ticket      implicit authentication ticket
   * @return Map[String, String] (property key -> default value)
   */
  def getObligatoryPropertyKeys(constraints: Seq[Constraint])(implicit ticket: Ticket): Map[String, String] = {
    RoleAssertion.assertWorker
    CollectibleLogic.getObligatoryPropertyKeys(constraints)
  }

}
