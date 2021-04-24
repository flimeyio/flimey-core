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
  def add(collection: Collection, newProperties: Seq[Property], newViewers: Seq[Viewer]): Future[Long] = {
    db.run((for {
      entityId <- (entities returning entities.map(_.id)) += FlimeyEntity(0)
      collectionId <- (collections returning collections.map(_.id)) += Collection(0, entityId, collection.typeVersionId, collection.status, collection.created)
      _ <- properties ++= newProperties.map(p => Property(0, p.key, p.value, entityId))
      _ <- viewers ++= newViewers.map(v => Viewer(0, entityId, v.viewerId, v.role))
    } yield collectionId).transactionally)
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
   * Update the state attribute of a [[modules.subject.model.Collection Collection]]
   *
   * @param collectionId id of the Collection to update
   * @param newState     new state string state value
   * @return Future[Int]
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

    //run the sub query to get all collections which are not archived and can be accessed.
    //those collections are used in separate queries to aggregate all necessary associated data.
    db.run(collections.filter(_.id in subQuery).filter(_.status =!= SubjectState.ARCHIVED.toString).sortBy(_.id.asc).result) flatMap( accessableCollections => {

      val accessableCollectionIds = accessableCollections.map(_.id)
      val accessableCollectionEntityIds = accessableCollections.map(_.entityId)
      val accessableCollectionTypeVersionIds = accessableCollections.map(_.typeVersionId)

      val propertyQuery = properties.filter(_.parentId inSet accessableCollectionEntityIds)
      val viewerQuery = viewers.filter(_.targetId inSet accessableCollectionEntityIds) join groups on (_.viewerId === _.id)
      val typeQuery = typeVersions.filter(_.id inSet accessableCollectionTypeVersionIds) join entityTypes on (_.typeId === _.id)
      val collectibleQuery = collectibles.filter(_.collectionId inSet accessableCollectionIds) join properties on (_.entityId === _.parentId)

      for {
        propertyResult <- db.run(propertyQuery.result)
        viewerResult <- db.run(viewerQuery.result)
        collectibleResult <- db.run(collectibleQuery.result)
        typeResult <- db.run(typeQuery.result)
      } yield {
        accessableCollections.map(collection => {

          val properties = propertyResult.filter(_.parentId == collection.entityId).sortBy(_.id)
          val viewerRelations = viewerResult.filter(_._1.targetId == collection.entityId).map(_.swap)
          val collectiblesData = collectibleResult.filter(_._1.collectionId == collection.id).groupBy(_._1).mapValues(values => values.map(_._2))
          val entityType = typeResult.find(_._1.id == collection.typeVersionId).get._2

          val collectibles: Seq[CollectibleHeader] = parseCollectibles(collectiblesData)

          CollectionHeader(collection, collectibles, properties, ViewerCombinator.fromRelations(viewerRelations), Some(entityType))

        }).sortBy(_.collection.id)
      }
    })
  }

  /**
   * TODO add doc
   * @param collectiblesData
   * @return
   */
  private def parseCollectibles(collectiblesData: Map[Collectible, Seq[Property]]): Seq[CollectibleHeader] = {
    collectiblesData.keys.map(collectible => {
      val properties = collectiblesData(collectible).sortBy(_.id)
      CollectibleHeader(collectible, properties, None)
    }).toSeq.sortBy(_.collectible.id)
  }

  /**
   * TODO add doc
   * @param nameQuery
   * @param groupIds
   * @return
   */
  def findArchivedCollections(nameQuery: String, groupIds: Set[Long]): Future[Seq[ArchivedCollection]] = {
    val accessQuery = (for {
      (c, s) <- collections join viewers.filter(_.viewerId.inSet(groupIds)) on (_.entityId === _.targetId)
    } yield (c, s)).groupBy(_._1.id).map(_._1)

    val resultIDQuery = (collections.filter(_.id in accessQuery).filter(_.status === SubjectState.ARCHIVED.toString) join
      properties.filter(_.key === "Name").filter(_.value like s"%$nameQuery%") on (_.entityId === _.parentId)).map(_._1.id).take(256)

    val resultQuery = collections.filter(_.id in resultIDQuery) join properties on(_.entityId === _.parentId)

    db.run(resultQuery.result).map(res => {
      val collectionsWithProperties = res.groupBy(_._1).mapValues(values => values.map(_._2))
      collectionsWithProperties.keys.toSeq.sortBy(_.id).map(collection => {
        val properties = collectionsWithProperties(collection)
        ArchivedCollection(collection, properties.sortBy(_.id))
      })
    })
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
    val typeQuery = collectionQuery join typeVersions on (_.typeVersionId === _.id) join entityTypes on (_._2.typeId === _.id)

    //fetch all collectibles with properties
    val collectibleQuery = collectionQuery join (collectibles join properties on (_.entityId === _.parentId)) on (_.id === _._1.collectionId)
    val collectibleTypeQuery = collectionQuery join collectibles on (_.id === _.collectionId) join
      typeVersions on (_._2.typeVersionId === _.id) join entityTypes on (_._2.typeId === _.id)

    for {
      propertyResult <- db.run(propertyQuery.result)
      viewerResult <- db.run(viewerQuery.result)
      collectibleResult <- db.run(collectibleQuery.result)
      typeResult <- db.run(typeQuery.result)
      collectibleTypeResult <- db.run(collectibleTypeQuery.result)
    } yield {
      val collectionWithProperties = propertyResult.groupBy(_._1).mapValues(values => values.map(_._2)).headOption
      val collectionWithViewers = viewerResult.groupBy(_._1).mapValues(values => values.map(_._2)).headOption
      val collectionWithCollectibleData = collectibleResult.groupBy(_._1).mapValues(values =>
        values.map(_._2).groupBy(_._1).mapValues(cValues => cValues.map(_._2)))
      val collectionWithType = typeResult.groupBy(_._1._1).mapValues(_.head._2)
      val collectiblesWithTypes = collectibleTypeResult.groupBy(_._1._1._2).mapValues(_.head._2)

      if (collectionWithViewers.isEmpty) {
        None
      } else {
        val collection = collectionWithProperties.get._1
        var collectibles: Seq[CollectibleHeader] = Seq()
        val entityType = collectionWithType(collection)
        if (collectionWithCollectibleData.contains(collection)) collectibles = parseCollectibles(collectionWithCollectibleData(collection))
        collectibles = collectibles.map(value => CollectibleHeader(value.collectible, value.properties, collectiblesWithTypes.get(value.collectible)))
        Some(ExtendedCollection(
          collection,
          collectibles,
          collectionWithProperties.get._2.filter(_.isDefined).map(_.get),
          Seq(), //TODO Attachments
          ViewerCombinator.fromRelations(collectionWithViewers.get._2),
          entityType))
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

    val propertyQuery = collectionQuery joinLeft properties on (_.entityId === _.parentId)
    val viewerQuery = collectionQuery join (groups join viewers on (_.id === _.viewerId)) on (_.entityId === _._2.targetId)

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
          collectionWithProperties.get._2.filter(_.isDefined).map(_.get),
          ViewerCombinator.fromRelations(collectionWithViewers.get._2),
          None))
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

  /**
   * Delete a [[modules.core.model.TypeVersion TypeVersion]] of a [[modules.subject.model.Collection Collection]].
   * <p> To ensure integrity, this operation deletes:
   * <p> 1. all [[modules.core.model.Constraint Constraints]] of the type.
   * <p> 2. all [[modules.core.model.FlimeyEntity Entities (Collections)]] which use this type...
   * <p> 3. ... with all their [[modules.core.model.Property Properties]].
   * <p> 4. all to Entities of this type associated [[modules.core.model.Viewer Viewers]].
   *
   * @param typeVersionId id of the [[modules.core.model.TypeVersion TypeVersion]]
   * @return Future[Unit]
   */
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
   * Add new [[modules.core.model.Constraint Constraints]] to a [[modules.core.model.TypeVersion TypeVersion]] of
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
   * @param typeVersionId       id of the parent [[modules.core.model.TypeVersion TypeVersion]]
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