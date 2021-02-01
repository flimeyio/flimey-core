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
import modules.core.repository.{PropertyRepository, TypeRepository}
import modules.subject.model._
import modules.subject.repository.CollectionRepository
import modules.user.model.GroupStats
import modules.user.service.GroupService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The service class to provide safe functionality to work with Collections.
 * <p> Normally, this class is used with dependency injection in controller classes or as helper in other services.
 *
 * @param typeRepository         injected [[modules.core.repository.TypeRepository TypeRepository]]
 * @param collectionRepository   injected [[modules.subject.repository.CollectionRepository CollectionRepository]]
 * @param propertyRepository     injected [[modules.core.repository.PropertyRepository PropertyRepository]]
 * @param modelCollectionService injected [[modules.subject.service.ModelCollectionService]]
 * @param groupService           injected [[modules.user.service.GroupService]]
 */
class CollectionService @Inject()(typeRepository: TypeRepository,
                                  collectionRepository: CollectionRepository,
                                  propertyRepository: PropertyRepository,
                                  modelCollectionService: ModelCollectionService,
                                  groupService: GroupService) {

  /**
   * Add a new [[modules.subject.model.Collection Collection]].
   * <p> Invalid and duplicate names in maintainers, editors and viewers is filtered out and does not lead to exceptions.
   * <p> <strong> Note: a User (defined by his ticket) can create Collections he is unable to access himself (by assigning other Groups)</strong>
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param typeId       id of the Collection [[modules.core.model.EntityType]]
   * @param propertyData of the new Collection (must complete the Collection EntityType model)
   * @param maintainers  names of Groups to serve as maintainers
   * @param editors      names of Groups to serve as editors
   * @param viewers      names of Groups to serve as viewers
   * @param ticket       implicit authentication ticket
   * @return Future[Unit]
   */
  def addCollection(typeId: Long, propertyData: Seq[String], maintainers: Seq[String], editors: Seq[String],
               viewers: Seq[String])(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertWorker
      typeRepository.getComplete(typeId, Some(CollectionConstraintSpec.COLLECTION)) flatMap (typeData => {
        val (head, constraints) = typeData
        if (!(head.isDefined && head.get.active)) throw new Exception("The selected Collection Type is not defined or active")
        val properties = CollectionLogic.derivePropertiesFromRawData(constraints, propertyData)
        val configurationStatus = CollectionLogic.isModelConfiguration(constraints, properties)
        if (!configurationStatus.valid) configurationStatus.throwError
        groupService.getAllGroups flatMap (allGroups => {
          val aViewers = CollectionLogic.deriveViewersFromData(maintainers :+ GroupStats.SYSTEM_GROUP, editors, viewers, allGroups)
          //FIXME validate status and move creation to CollectionLogic object
          collectionRepository.add(Collection(0, 0, typeId, SubjectStatus.CREATED, Timestamp.from(Instant.now())), properties, aViewers)
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  ///**
  // * Update a [[modules.subject.model.Collection Collection]] with its [[modules.core.model.Property Properties]]
  // * and [[modules.core.model.Viewer Viewers]].
  // * <p> All Properties (if updated or not) must be passed, else the configuration can not be verified.
  // * <p> All Viewers (old AND new ones) must be passed as string. Old viewers that are not passed will be deleted.
  // * Invalid and duplicate Viewer names (Group names) are filtered out. Only the highest role is applied per Viewer.
  // * The SYSTEM Group can not be removed as MAINTAINER.
  // * <p> Fails without WORKER rights.
  // * <p> If Properties are changed, EDITOR rights are required.
  // * <p> If Viewers are changed, MAINTAINER rights are required.
  // * <p> This is a safe implementation and can be used by controller classes.
  // *
  // * @param collectionId       id of the Collection to update
  // * @param propertyUpdateData all Properties of the changed Collection (can contain updated values)
  // * @param maintainers        all (old and new) Group names of Viewers with role MAINTAINER
  // * @param editors            all (old and new) Group names of Viewers with role EDITOR
  // * @param viewers            all (old and new) Group names of Viewers with role VIEWER
  // * @param ticket             implicit authentication ticket
  // * @return Future[Unit]
  // */
  //def updateCollection(collectionId: Long, propertyUpdateData: Seq[String], maintainers: Seq[String], editors: Seq[String],
  //                viewers: Seq[String])(implicit ticket: Ticket): Future[Unit] = {
  //  try {
  //    //TODO
  //  } catch {
  //    case e: Throwable => Future.failed(e)
  //  }
  //}

  ///**
  // * Get an [[modules.subject.model.ExtendedCollection ExtendedCollection]] together with its
  // * [[modules.core.model.ExtendedEntityType ExtendedEntityType]] by its id.
  // * <p> A User (given by his ticket) can only request Collections he has access rights to.
  // * <p> Fails without WORKER rights.
  // * <p> This is a safe implementation and can be used by controller classes.
  // *
  // * @param collectionId id of the Asset to fetch
  // * @param ticket  implicit authentication ticket
  // * @return Future[(ExtendedCollection, ExtendedEntityType)]
  // */
  //def getCollection(collectionId: Long)(implicit ticket: Ticket): Future[(ExtendedCollection, ExtendedEntityType)] = {
  //  try {
  //    //TODO
  //  } catch {
  //    case e: Throwable => Future.failed(e)
  //  }
  //}

  /**
   * Get all [[modules.subject.model.CollectionHeader CollectionHeaders]]
   * <p> Only Collection data the given User (by ticket) can access is returned.
   * <p> Fails without WORKER rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param groupSelector groups which must contain the returned Collection data (must be partition of ticket Groups)
   * @param ticket        implicit authentication ticket
   * @return Future Seq[ExtendedCollection]
   */
  def getCollectionHeaders(typeSelector: Option[String] = None, groupSelector: Option[String] = None)(implicit ticket: Ticket): Future[Seq[CollectionHeader]] = {
    try {
      RoleAssertion.assertWorker
      var accessedGroupIds = ticket.accessRights.getAllViewingGroupIds
      if (groupSelector.isDefined) {
        val selectedGroups = CollectionLogic.splitNumericList(groupSelector.get)
        accessedGroupIds = accessedGroupIds.filter(!selectedGroups.contains(_))
      }
      //FIXME handle type selector (best would be a "exclude type" selector)
      collectionRepository.getCollectionHeaders(accessedGroupIds)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all [[modules.subject.model.CollectionHeader CollectionHeaders]] and all
   * [[modules.core.model.ExtendedEntityType ExtendedEntityTypes]] which define them.
   * <p> This method calls [[modules.subject.service.CollectionService#getCollectionHeaders]] (see there for more information)
   * <p> This method calls [[modules.subject.service.ModelCollectionService#getAllExtendedTypes]] (see there for more information)
   * <p> Fails without WORKER rights.
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param groupSelector groups which must contain the returned Collection (must be partition of ticket Groups)
   * @param ticket        implicit authentication ticket
   * @return Future[CollectionTypeComplex]
   */
  def getCollectionComplex(typeSelector: Option[String] = None, groupSelector: Option[String] = None)(implicit ticket: Ticket):
  Future[CollectionTypeComplex] = {
    try {
      for {
        collectionHeaders <- getCollectionHeaders(typeSelector, groupSelector)
        extendedEntityTypes <- modelCollectionService.getAllExtendedTypes()
      } yield {
        CollectionTypeComplex(collectionHeaders, extendedEntityTypes)
      }
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  ///**
  // * Delete a [[modules.subject.model.Collection Collection]].
  // * <p> <strong> This will also delete all child [[modules.subject.model.Collectible Collectibles]]!</strong>
  // * <p> This is a safe implementation and can be used by controller classes.
  // * <p> Fails without MAINTAINER rights
  // *
  // * @param id     of the Collection
  // * @param ticket implicit authentication ticket
  // * @return Future[Unit]
  // */
  //def deleteCollection(id: Long)(implicit ticket: Ticket): Future[Unit] = {
  //  try {
  //    //TODO
  //  } catch {
  //    case e: Throwable => Future.failed(e)
  //  }
  //}

  /**
   * Forwards to same method of [[modules.subject.service.CollectionLogic CollectionLogic]].
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without WORKER rights
   *
   * @param constraints model of an CollectionType
   * @param ticket      implicit authentication ticket
   * @return Seq[(String, String)] (property key -> data type)
   */
  def getCollectionPropertyKeys(constraints: Seq[Constraint])(implicit ticket: Ticket): Seq[(String, String)] = {
    RoleAssertion.assertWorker
    CollectionLogic.getPropertyKeys(constraints)
  }

  /**
   * Forwards to same method of [[modules.subject.service.CollectionLogic CollectionLogic]].
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without WORKER rights
   *
   * @param constraints model of an CollectionType
   * @param ticket      implicit authentication ticket
   * @return Map[String, String] (property key -> default value)
   */
  def getObligatoryPropertyKeys(constraints: Seq[Constraint])(implicit ticket: Ticket): Map[String, String] = {
    RoleAssertion.assertWorker
    CollectionLogic.getObligatoryPropertyKeys(constraints)
  }

}
