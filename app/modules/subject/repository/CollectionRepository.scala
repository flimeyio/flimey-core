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

package modules.subject.repository

import com.google.inject.Inject
import modules.core.model.{Constraint, FlimeyEntity, Property, Viewer}
import modules.core.repository._
import modules.subject.model._
import modules.user.model.ViewerCombinator
import modules.user.repository.GroupTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

/**
 * Repository to perform database operation on [[modules.subject.model.Collection Collection]] and associated entities.
 *
 * @param dbConfigProvider injected db configuration
 * @param executionContext implicit ExecutionContext
 */
class CollectionRepository @Inject()(@NamedDatabase("flimey_data") protected val dbConfigProvider: DatabaseConfigProvider)(
  implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val collections = TableQuery[CollectionTable]
  val collectibles = TableQuery[CollectibleTable]
  val entities = TableQuery[FlimeyEntityTable]
  val entityTypes = TableQuery[TypeTable]
  val typeVersions = TableQuery[TypeVersionTable]
  val constraints = TableQuery[ConstraintTable]
  val properties = TableQuery[PropertyTable]
  val viewers = TableQuery[ViewerTable]
  val groups = TableQuery[GroupTable]

  /**
   * Add a new [[modules.subject.model.Collection Collection]] with [[modules.core.model.Property Properties]] to the db.<br />
   * The Collection id and Property ids are set to 0 to enable auto increment.
   * <p> Only valid Collection configurations should be added to the repository.
   *
   * @param collection    new Collection entity
   * @param newProperties Properties of the Collection
   * @param newViewers    [[modules.core.model.Viewer Viewers]] of the Collection.
   * @return Future[Unit]
   */
  def add(collection: Collection, newProperties: Seq[Property], newViewers: Seq[Viewer]): Future[Unit] = {
    db.run((for {
      entityId <- (entities returning entities.map(_.id)) += FlimeyEntity(0)
      _ <- (collections returning collections.map(_.id)) += Collection(0, entityId, collection.typeVersionId, collection.status, collection.created)
      _ <- properties ++= newProperties.map(p => Property(0, p.key, p.value, entityId))
      _ <- viewers ++= newViewers.map(v => Viewer(0, entityId, v.viewerId, v.role))
    } yield ()).transactionally)
  }

  /**
   * Delete a [[modules.subject.model.Collection Collection]] and all associated data from the db.
   * <p> Deletes also all [[modules.subject.model.Collectible Collectibles]]!
   *
   * @param collection the Collection to delete
   * @return Future[Unit]
   */
  def delete(collection: Collection): Future[Unit] = {
    val collectibleEntityIDsToDelete = collectibles.filter(_.collectionId === collection.id).map(_.entityId)
    db.run((for {
      _ <- properties.filter(_.parentId === collection.entityId).delete
      _ <- viewers.filter(_.targetId === collection.entityId).delete
      _ <- properties.filter(_.parentId in collectibleEntityIDsToDelete).delete
      _ <- collectibles.filter(_.entityId in collectibleEntityIDsToDelete).delete
      _ <- entities.filter(_.id in collectibleEntityIDsToDelete).delete
      //TODO delete attachments here
      _ <- collections.filter(_.id === collection.id).delete
      _ <- entities.filter(_.id === collection.entityId).delete
    } yield ()).transactionally)
  }

  /**
   * TODO add doc
   *
   * @param collectionId
   * @param newState
   * @return
   */
  def updateState(collectionId: Long, newState: SubjectState.State): Future[Int] = {
    db.run(collections.filter(_.id === collectionId).map(_.status).update(newState.toString))
  }

  /**
   * Get all [[modules.subject.model.CollectionHeader CollectionHeaders]] which are accessible by the given
   * [[modules.user.model.Group Groups]].
   * <p> Note that this operation is not limited so that large amounts of data could be returned.
   *
   * @param groupIds groups which must have access to the selected Collections
   * @return Future Seq[CollectionHeader]
   */
  def getCollectionHeaders(groupIds: Set[Long]): Future[Seq[CollectionHeader]] = {
    //build sub-query to get all ids of collections which can be accessed by the given groups
    val subQuery = (for {
      (c, s) <- collections join viewers.filter(_.viewerId.inSet(groupIds)) on (_.entityId === _.targetId)
    } yield (c, s)).groupBy(_._1.id).map(_._1)

    val accessableCollections = collections.filter(_.id in subQuery).sortBy(_.id.asc)

    //fetch all properties
    val propertyQuery = accessableCollections joinLeft properties on (_.entityId === _.parentId)

    //fetch all viewers
    val viewerQuery = accessableCollections join (groups join viewers on (_.id === _.viewerId)) on (_.entityId === _._2.targetId)

    //fetch all collectibles with properties
    val collectibleQuery = accessableCollections join (collectibles join properties on (_.entityId === _.parentId)) on (_.id === _._1.collectionId)

    for {
      propertyResult <- db.run(propertyQuery.result)
      viewerResult <- db.run(viewerQuery.result)
      collectibleResult <- db.run(collectibleQuery.result)
    } yield {
      val collectionsWithProperties = propertyResult.groupBy(_._1).mapValues(values => values.map(_._2))
      val collectionsWithViewers = viewerResult.groupBy(_._1).mapValues(values => values.map(_._2))
      val collectionsWithCollectibleData = collectibleResult.groupBy(_._1).mapValues(values =>
        values.map(_._2).groupBy(_._1).mapValues(cValues => cValues.map(_._2)))

      collectionsWithProperties.keys.map(collection => {
        val properties = collectionsWithProperties(collection).filter(_.isDefined).map(_.get)
        val viewerRelations = collectionsWithViewers(collection)
        val collectiblesOption = collectionsWithCollectibleData.get(collection)

        var collectibles: Seq[CollectibleHeader] = Seq()
        if (collectiblesOption.isDefined) collectibles = parseCollectibles(collectiblesOption.get)

        CollectionHeader(collection, collectibles, properties, ViewerCombinator.fromRelations(viewerRelations))

      }).toSeq.sortBy(_.collection.id)
    }
  }

  private def parseCollectibles(collectiblesData: Map[Collectible, Seq[Property]]): Seq[CollectibleHeader] = {
    collectiblesData.keys.map(collectible => {
      val properties = collectiblesData(collectible).sortBy(_.id)
      CollectibleHeader(collectible, properties)
    }).toSeq.sortBy(_.collectible.id)
  }

  /**
   * Get a single [[modules.subject.model.ExtendedCollection ExtendedCollection]] by its id. The given
   * [[modules.user.model.Group Group]] ids must give access rights to the [[modules.subject.model.Collection Collection]].
   * <p> If the id does not exists or there are no access rights, nothing is returned.
   *
   * @param collectionId id of the Collection to get
   * @param groupIds     Group ids of which at least one must have access to the Collection
   * @return Future Option[ExtendedCollection]
   */
  def getCollection(collectionId: Long, groupIds: Set[Long]): Future[Option[ExtendedCollection]] = {

    val accessQuery = (for {
      (c, s) <- collections.filter(_.id === collectionId) join viewers.filter(_.viewerId.inSet(groupIds)) on (_.entityId === _.targetId)
    } yield (c, s)).groupBy(_._1.id).map(_._1)

    val collectionQuery = collections.filter(_.id in accessQuery)

    val propertyQuery = collectionQuery joinLeft properties on (_.entityId === _.parentId)

    val viewerQuery = collectionQuery join (groups join viewers on (_.id === _.viewerId)) on (_.entityId === _._2.targetId)

    //fetch all collectibles with properties
    val collectibleQuery = collectionQuery join (collectibles join properties on (_.entityId === _.parentId)) on (_.id === _._1.collectionId)

    for {
      propertyResult <- db.run(propertyQuery.result)
      viewerResult <- db.run(viewerQuery.result)
      collectibleResult <- db.run(collectibleQuery.result)
    } yield {
      val collectionWithProperties = propertyResult.groupBy(_._1).mapValues(values => values.map(_._2)).headOption
      val collectionWithViewers = viewerResult.groupBy(_._1).mapValues(values => values.map(_._2)).headOption
      val collectionWithCollectibleData = collectibleResult.groupBy(_._1).mapValues(values =>
        values.map(_._2).groupBy(_._1).mapValues(cValues => cValues.map(_._2)))

      if (collectionWithViewers.isEmpty) {
        None
      } else {
        val collection = collectionWithProperties.get._1
        var collectibles: Seq[CollectibleHeader] = Seq()
        if (collectionWithCollectibleData.contains(collection)) collectibles = parseCollectibles(collectionWithCollectibleData(collection))
        Some(ExtendedCollection(
          collection,
          collectibles,
          collectionWithProperties.get._2.filter(_.isDefined).map(_.get),
          Seq(), //TODO Attachments --> May be moved to AttachmentRepository somehow
          ViewerCombinator.fromRelations(collectionWithViewers.get._2)))
      }
    }
  }

  /**
   * Get a single [[modules.subject.model.CollectionHeader CollectionHeader]] WITHOUT Collectible data by its id. The given
   * [[modules.user.model.Group Group]] ids must give access rights to the [[modules.subject.model.Collection Collection]].
   * <p> If the id does not exists or there are no access rights, nothing is returned.
   *
   * @param collectionId id of the Collection to get
   * @param groupIds     Group ids of which at least one must have access to the Collection
   * @return Future Option[CollectionHeader]
   */
  def getSlimCollection(collectionId: Long, groupIds: Set[Long]): Future[Option[CollectionHeader]] = {

    val accessQuery = (for {
      (c, s) <- collections.filter(_.id === collectionId) join viewers.filter(_.viewerId.inSet(groupIds)) on (_.entityId === _.targetId)
    } yield (c, s)).groupBy(_._1.id).map(_._1)

    val collectionQuery = collections.filter(_.id in accessQuery)

    val propertyQuery = for {
      c <- collectionQuery join properties on (_.entityId === _.parentId)
    } yield c

    val viewerQuery = for {
      c <- collectionQuery join (groups join viewers on (_.id === _.viewerId)) on (_.entityId === _._2.targetId)
    } yield c

    for {
      propertyResult <- db.run(propertyQuery.result)
      viewerResult <- db.run(viewerQuery.result)
    } yield {
      val collectionWithProperties = propertyResult.groupBy(_._1).mapValues(values => values.map(_._2)).headOption
      val collectionWithViewers = viewerResult.groupBy(_._1).mapValues(values => values.map(_._2)).headOption

      if (collectionWithProperties.isEmpty) {
        None
      } else {
        Some(CollectionHeader(
          collectionWithProperties.get._1,
          Seq(), //No Collectibles here - that's the slim part ;)
          collectionWithProperties.get._2,
          ViewerCombinator.fromRelations(collectionWithViewers.get._2)))
      }
    }
  }

  /**
   * Delete a [[modules.core.model.EntityType EntityType]] of a [[modules.subject.model.Collection Collection]].
   * <p> To ensure integrity, this operation deletes:
   * <p> 1. all [[modules.core.model.Constraint Constraints]] of the type.
   * <p> 2. all [[modules.core.model.FlimeyEntity Entities (Collections)]] which use this type...
   * <p> 3. ... with all their [[modules.core.model.Property Properties]].
   * <p> 4. all to Entities of this type associated [[modules.core.model.Viewer Viewers]].
   *
   * @param typeId of the EntityType (CollectionType) to delete
   * @return Future[Unit]
   */
  def deleteCollectionType(typeId: Long): Future[Unit] = {
    val typeVersionsToDeleteIds = typeVersions.filter(_.typeId === typeId).map(_.id)
    val collectionsOfTypeEntityIds = collections.filter(_.typeVersionId in typeVersionsToDeleteIds).map(_.entityId)
    db.run((for {
      _ <- properties.filter(_.parentId in collectionsOfTypeEntityIds).delete
      _ <- viewers.filter(_.targetId in collectionsOfTypeEntityIds).delete
      _ <- collections.filter(_.entityId in collectionsOfTypeEntityIds).delete
      _ <- entities.filter(_.id in collectionsOfTypeEntityIds).delete
      _ <- constraints.filter(_.typeVersionId in typeVersionsToDeleteIds).delete
      _ <- typeVersions.filter(_.id in typeVersionsToDeleteIds).delete
      _ <- entityTypes.filter(_.id === typeId).delete
    } yield ()).transactionally)
  }

  def deleteCollectionTypeVersion(typeVersionId: Long): Future[Unit] = {
    val collectionsToDeleteEntityIds = collections.filter(_.typeVersionId === typeVersionId).map(_.entityId)
    db.run((for {
      _ <- properties.filter(_.parentId in collectionsToDeleteEntityIds).delete
      _ <- viewers.filter(_.targetId in collectionsToDeleteEntityIds).delete
      _ <- collections.filter(_.entityId in collectionsToDeleteEntityIds).delete
      _ <- entities.filter(_.id in collectionsToDeleteEntityIds).delete
      _ <- constraints.filter(_.typeVersionId === typeVersionId).delete
      _ <- typeVersions.filter(_.id === typeVersionId).delete
    } yield ()).transactionally)
  }

  /**
   * Add new [[modules.core.model.Constraint Constraints]] to a [[modules.core.model.FlimeyEntity FlimeyEntity]] of
   * the [[modules.subject.model.Collection Collection]] subtype.
   * <p> The id of all new Constraints must be set to 0 to enable auto increment.
   * <p> This method makes a difference between new propertyConstraints (Constraints of the HasProperty type) and other
   * Constraints.
   * <p> <strong>If you add new HasProperty Constraints the wrong way (via otherConstraints or just
   * [[modules.core.repository.ConstraintRepository#addConstraint]]) will lead to loosing the integrity of the type
   * system. </strong>
   *
   * @param typeVersionId       id of the TypeVersion (of a Collection) to add the new Constraints to.
   * @param propertyConstraints new Constraints of HasProperty type
   * @param otherConstraints    new Constraints NOT of HasProperty type
   * @return Future[Unit]
   */
  def addConstraints(typeVersionId: Long, propertyConstraints: Seq[Constraint], otherConstraints: Seq[Constraint]): Future[Unit] = {

    val allConstraints = (propertyConstraints ++ otherConstraints) map (c => Constraint(c.id, c.c, c.v1, c.v2, c.byPlugin, typeVersionId))

    db.run((for {
      entityIDsWithType <- collections.filter(_.typeVersionId === typeVersionId).map(_.entityId).result
      _ <- DBIO.sequence(propertyConstraints.map(propertyConstraint => {
        properties ++= entityIDsWithType.map(entityId => Property(0, propertyConstraint.v1, "", entityId))
      }))
      _ <- (constraints returning constraints.map(_.id)) ++= allConstraints
    } yield ()).transactionally)
  }

  /**
   * Delete [[modules.core.model.Constraint Constraints]] of a [[modules.subject.model.Collection Collection]] associated
   * [[modules.core.model.EntityType EntityType]].
   * <p> This operation deletes all [[modules.core.model.Property Properties]] associated to HasProperty Constraints
   * (here represented by propertyConstraints seq)
   * <p> The otherConstraints must contain Constraints which are not of HasProperty type.
   * <p> <strong> If HasProperty Constraints are not deleted separately (by putting them in otherConstraints or just
   * calling [[modules.core.repository.ConstraintRepository#deleteConstraint]]) the type system of the database will
   * be damaged and the system becomes unusable!</strong>
   *
   * @param typeVersionId       id of the parent TypeVersion
   * @param propertyConstraints Constraints to delete of the HasProperty type
   * @param otherConstraints    Constraints to delete NOT of the HasProperty type
   * @return Future[Unit]
   */
  def deleteConstraints(typeVersionId: Long, propertyConstraints: Seq[Constraint], otherConstraints: Seq[Constraint]): Future[Unit] = {

    val deletedPropertyKeys = propertyConstraints.map(_.v1) toSet
    val deletedConstraintIds = propertyConstraints ++ otherConstraints map (_.id) toSet

    val entitiesIDsWithType = collections.filter(_.typeVersionId === typeVersionId).map(_.entityId)

    db.run((for {
      _ <- properties.filter(_.parentId in entitiesIDsWithType).filter(_.key.inSet(deletedPropertyKeys)).delete
      _ <- constraints.filter(_.id.inSet(deletedConstraintIds)).delete
    } yield ()).transactionally)
  }

}
