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
import modules.core.repository.{FlimeyEntityRepository, PropertyRepository, TypeRepository, ViewerRepository}
import modules.subject.model.{Collectible, CollectibleConstraintSpec, CollectionConstraintSpec, SubjectState}
import modules.subject.repository.{CollectibleRepository, CollectionRepository}
import modules.user.model.GroupStats
import modules.user.service.GroupService
import modules.user.util.ViewerAssertion

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * The service class to provide safe functionality to work with [[modules.subject.model.Collectible Collectibles]].
 * <p> Normally, this class is used with dependency injection in controller classes or as helper in other services.
 *
 * @param typeRepository         injected [[modules.core.repository.TypeRepository TypeRepository]]
 * @param collectionRepository   injected [[modules.subject.repository.CollectionRepository CollectionRepository]]
 * @param propertyRepository     injected [[modules.core.repository.PropertyRepository PropertyRepository]]
 * @param entityRepository
 * @param viewerRepository
 * @param modelCollectionService injected [[modules.subject.service.ModelCollectionService]]
 * @param groupService           injected [[modules.user.service.GroupService]]
 */
class CollectibleService @Inject()(typeRepository: TypeRepository,
                                   collectibleRepository: CollectibleRepository,
                                   propertyRepository: PropertyRepository,
                                   entityRepository: FlimeyEntityRepository,
                                   viewerRepository: ViewerRepository,
                                   modelCollectionService: ModelCollectionService,
                                   collectionService: CollectionService,
                                   groupService: GroupService) {

  /**
   * Add a new [[modules.subject.model.Collectible Collectible]].
   * <p> Fails without WORKER rights.
   * <p> Requires EDITOR rights in the parent Collection.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param collectionId id of the parent [[modules.subject.model.Collection Collection]]
   * @param typeId       id of the Collectible [[modules.core.model.EntityType]]
   * @param propertyData of the new Collectible (must complete the Collectible EntityType model)
   * @param ticket       implicit authentication ticket
   * @return Future[Unit]
   */
  def addCollectible(collectionId: Long, typeId: Long, propertyData: Seq[String])(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertWorker
      collectionService.getSlimCollection(collectionId) flatMap (collectionHeader => {
        ViewerAssertion.assertEdit(collectionHeader.viewers)
        for {
          collectionTypeData <- typeRepository.getComplete(collectionHeader.collection.typeId, Some(CollectionConstraintSpec.COLLECTION))
          collectibleTypeData <- typeRepository.getComplete(typeId, Some(CollectibleConstraintSpec.COLLECTIBLE))
        } yield {
          val (collectionTypeOption, collectionConstraints) = collectionTypeData
          val (collectibleTypeOption, collectibleConstraints) = collectibleTypeData
          if (!(collectionTypeOption.isDefined && collectionTypeOption.get.active)) throw new Exception("The selected Collection Type is not defined or active")
          if (!(collectibleTypeOption.isDefined && collectibleTypeOption.get.active)) throw new Exception("The selected Collectible Type is not defined or active")
          val collectibleType = collectibleTypeOption.get
          val extensionStatus = CollectibleLogic.canBeChildOf(collectibleType, collectionConstraints)
          if(!extensionStatus.valid) extensionStatus.throwError

          val properties = CollectibleLogic.derivePropertiesFromRawData(collectibleConstraints, propertyData)
            val configurationStatus = CollectibleLogic.isModelConfiguration(collectibleConstraints, properties)
            if (!configurationStatus.valid) configurationStatus.throwError

            collectibleRepository.add(
              Collectible(0, 0, collectionHeader.collection.id, typeId, SubjectState.CREATED, Timestamp.from(Instant.now())), properties)

        }
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  ///**
  // * Update a [[modules.subject.model.Collectible Collectible]] with its [[modules.core.model.Property Properties]].
  // * <p> All Properties (if updated or not) must be passed, else the configuration can not be verified.
  // * <p> Fails without WORKER rights.
  // * <p> If Properties are changed, EDITOR rights are required.
  // * <p> This is a safe implementation and can be used by controller classes.
  // *
  // * @param collectibleId      id of the Collectible to update
  // * @param propertyUpdateData all Properties of the changed Collectible (can contain updated values)
  // * @param ticket             implicit authentication ticket
  // * @return Future[Unit]
  // */
  //def updateCollectible(collectibleId: Long, propertyUpdateData: Seq[String])(implicit ticket: Ticket): Future[Unit] = {
  //  try {
  //    RoleAssertion.assertWorker
  //    //getSlimCollection(collectionId) flatMap (collectionHeader => {
  //    //
  //    //  //Check if the User can edit this Collection
  //    //  ViewerAssertion.assertEdit(collectionHeader.viewers)
  //    //
  //    //  //Parse updated properties and verify the configuration
  //    //  val properties = collectionHeader.properties
  //    //  val oldConfig = properties
  //    //  val newConfig = CollectionLogic.mapConfigurations(oldConfig, propertyUpdateData)
  //    //
  //    //  //check if the EntityType of the Collection is active (else it can not be edited)
  //    //  typeRepository.getComplete(collectionHeader.collection.typeId) flatMap (typeData => {
  //    //    val (head, constraints) = typeData
  //    //    if (!(head.isDefined && head.get.active)) throw new Exception("The selected Collection Type is not active")
  //    //    val configurationStatus = CollectionLogic.isModelConfiguration(constraints, newConfig)
  //    //    if (!configurationStatus.valid) configurationStatus.throwError
  //    //
  //    //    groupService.getAllGroups flatMap (groups => {
  //    //
  //    //      val (viewersToDelete, viewersToInsert) = CollectionLogic.getViewerChanges(
  //    //        maintainers.toSet + GroupStats.SYSTEM_GROUP,
  //    //        editors.toSet,
  //    //        viewers.toSet,
  //    //        collectionHeader.viewers,
  //    //        groups,
  //    //        collectionHeader.collection.id)
  //    //
  //    //      if (viewersToDelete.nonEmpty || viewersToInsert.nonEmpty) {
  //    //        ViewerAssertion.assertMaintain(collectionHeader.viewers)
  //    //      }
  //    //      entityRepository.update(newConfig, viewersToDelete, viewersToInsert)
  //    //    })
  //    //  })
  //    //})
  //  } catch {
  //    case e: Throwable => Future.failed(e)
  //  }
  //}

  ///**
  // * Update the [[modules.subject.model.SubjectState State]] of a [[modules.subject.model.Collectible Collectible]].
  // * <p> Fails without WORKER rights.
  // * <p> The requesting User must be EDITOR.
  // * <p> This is a safe implementation and can be used by controller classes.
  // *
  // * @param collectibleId id of the Collectible to update
  // * @param ticket        implicit authentication ticket
  // * @return Future[Unit]
  // */
  //def updateState(collectibleId: Long, newState: String)(implicit ticket: Ticket): Future[Int] = {
  //  try {
  //    RoleAssertion.assertWorker
  //    //getSlimCollection(collectionId) flatMap (collectionHeader => {
  //    //  //Check if the User can edit this Collection
  //    //  ViewerAssertion.assertEdit(collectionHeader.viewers)
  //    //  val state = CollectionLogic.parseState(newState);
  //    //  //FIXME if state can not be set to archived...
  //    //  val updateStatus = CollectionLogic.isValidStateTransition(collectionHeader.collection.status, state)
  //    //  if (!updateStatus.valid) updateStatus.throwError
  //    //  collectionRepository.updateState(collectionId, state)
  //    //})
  //  } catch {
  //    case e: Throwable => Future.failed(e)
  //  }
  //}

  ///**
  // * Get a [[modules.subject.model.ExtendedCollectible ExtendedCollectible]] together with its
  // * [[modules.core.model.Property Properties]] and [[modules.core.model.Viewer Viewers]] by id.
  // * <p> A User (given by his ticket) can only request Collectibles he has access rights to.
  // * <p> Fails without WORKER rights.
  // * <p> This is a safe implementation and can be used by controller classes.
  // *
  // * @param collectibleId id of the Collectible to fetch
  // * @param ticket        implicit authentication ticket
  // * @return Future[CollectionHeader]
  // */
  //def getCollectible(collectibleId: Long)(implicit ticket: Ticket): Future[ExtendedCollectible] = {
  //  try {
  //    RoleAssertion.assertWorker
  //    //val accessedGroupIds = ticket.accessRights.getAllViewingGroupIds
  //    //collectionRepository.getSlimCollection(collectionId, accessedGroupIds) map (collectionOption => {
  //    //  if (collectionOption.isEmpty) throw new Exception("Collection does not exist or missing rights")
  //    //  collectionOption.get
  //    //})
  //  } catch {
  //    case e: Throwable => Future.failed(e)
  //  }
  //}

  ///**
  // * Delete a [[modules.subject.model.Collectible Collectible]].
  // * <p> This is a safe implementation and can be used by controller classes.
  // * <p> Fails without MAINTAINER rights
  // *
  // * @param id     of the Collectible
  // * @param ticket implicit authentication ticket
  // * @return Future[Unit]
  // */
  //def deleteCollectible(id: Long)(implicit ticket: Ticket): Future[Unit] = {
  //  try {
  //    RoleAssertion.assertWorker
  //    //getSlimCollection(id) flatMap (collectionData => {
  //    //  ViewerAssertion.assertMaintain(collectionData.viewers)
  //    //  collectionRepository.delete(collectionData.collection)
  //    //})
  //  } catch {
  //    case e: Throwable => Future.failed(e)
  //  }
  //}

  ///**
  // * Forwards to same method of [[modules.subject.service.CollectibleLogic CollectibleLogic]].
  // * <p> This is a safe implementation and can be used by controller classes.
  // * <p> Fails without WORKER rights
  // *
  // * @param constraints model of an CollectibleType
  // * @param ticket      implicit authentication ticket
  // * @return Seq[(String, String)] (property key -> data type)
  // */
  //def getCollectiblePropertyKeys(constraints: Seq[Constraint])(implicit ticket: Ticket): Seq[(String, String)] = {
  //  RoleAssertion.assertWorker
  //  //CollectionLogic.getPropertyKeys(constraints)
  //}

  ///**
  // * Forwards to same method of [[modules.subject.service.CollectibleLogic CollectibleLogic]].
  // * <p> This is a safe implementation and can be used by controller classes.
  // * <p> Fails without WORKER rights
  // *
  // * @param constraints model of an CollectibleType
  // * @param ticket      implicit authentication ticket
  // * @return Map[String, String] (property key -> default value)
  // */
  //def getObligatoryPropertyKeys(constraints: Seq[Constraint])(implicit ticket: Ticket): Map[String, String] = {
  //  RoleAssertion.assertWorker
  //  //CollectionLogic.getObligatoryPropertyKeys(constraints)
  //}

}
