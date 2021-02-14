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
import modules.core.model.{Constraint, FlimeyEntity, Property}
import modules.core.repository._
import modules.subject.model.{Collectible, ExtendedCollectible, SubjectState}
import modules.user.model.ViewerCombinator
import modules.user.repository.GroupTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

/**
 * Repository to perform database operation on [[modules.subject.model.Collectible Collectible]] and associated entities.
 *
 * @param dbConfigProvider injected db configuration
 * @param executionContext implicit ExecutionContext
 */
class CollectibleRepository @Inject()(@NamedDatabase("flimey_data") protected val dbConfigProvider: DatabaseConfigProvider)(
  implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val collectibles = TableQuery[CollectibleTable]
  val collections = TableQuery[CollectionTable]
  val entities = TableQuery[FlimeyEntityTable]
  val entityTypes = TableQuery[TypeTable]
  val typeVersions = TableQuery[TypeVersionTable]
  val constraints = TableQuery[ConstraintTable]
  val properties = TableQuery[PropertyTable]
  val viewers = TableQuery[ViewerTable]
  val groups = TableQuery[GroupTable]

  /**
   * Add a new [[modules.subject.model.Collectible Collectible]] with [[modules.core.model.Property Properties]] to the db.<br />
   * The Collectible id and Property ids are set to 0 to enable auto increment.
   * <p> Only valid Collectible configurations should be added to the repository.
   *
   * @param collectible   new Collectible entity
   * @param newProperties Properties of the Collectible
   * @return Future[Unit]
   */
  def add(collectible: Collectible, newProperties: Seq[Property]): Future[Unit] = {
    db.run((for {
      entityId <- (entities returning entities.map(_.id)) += FlimeyEntity(0)
      _ <- (collectibles returning collectibles.map(_.id)) +=
        Collectible(0, entityId, collectible.collectionId, collectible.typeVersionId, collectible.state, collectible.created)
      _ <- properties ++= newProperties.map(p => Property(0, p.key, p.value, entityId))
    } yield ()).transactionally)
  }

  /**
   * Delete a [[modules.core.model.EntityType EntityType]] of a [[modules.subject.model.Collectible Collectible]].
   * <p> This deletes all subsidiary [[modules.core.model.TypeVersion TypeVersions]] of this type.
   * <p> To ensure integrity, this operation deletes:
   * <p> 1. all [[modules.core.model.Constraint Constraints]] of the type.
   * <p> 2. all [[modules.core.model.FlimeyEntity Entities (Collectibles)]] which use this type...
   * <p> 3. ... with all their [[modules.core.model.Property Properties]].
   *
   * @param typeId of the EntityType (Collectible Type) to delete
   * @return Future[Unit]
   */
  def deleteCollectibleType(typeId: Long): Future[Unit] = {
    val typeVersionsToDeleteIds = typeVersions.filter(_.typeId === typeId).map(_.id)
    val collectiblesOfTypeEntityIds = collectibles.filter(_.typeVersionId in typeVersionsToDeleteIds).map(_.entityId)
    db.run((for {
      _ <- properties.filter(_.parentId in collectiblesOfTypeEntityIds).delete
      _ <- collectibles.filter(_.entityId in collectiblesOfTypeEntityIds).delete
      _ <- entities.filter(_.id in collectiblesOfTypeEntityIds).delete
      _ <- constraints.filter(_.typeVersionId in typeVersionsToDeleteIds).delete
      _ <- typeVersions.filter(_.id in typeVersionsToDeleteIds).delete
      _ <- entityTypes.filter(_.id === typeId).delete
    } yield ()).transactionally)
  }

  /**
   * Delete a [[modules.core.model.TypeVersion TypeVersion]] of a [[modules.subject.model.Collectible Collectible]].
   * <p> To ensure integrity, this operation deletes:
   * <p> 1. all [[modules.core.model.Constraint Constraints]] of the type.
   * <p> 2. all [[modules.core.model.FlimeyEntity Entities (Collectibles)]] which use this type...
   * <p> 3. ... with all their [[modules.core.model.Property Properties]].
   *
   * @param typeVersionId of the TypeVersion to delete
   * @return Future[Unit]
   */
  def deleteCollectibleTypeVersion(typeVersionId: Long): Future[Unit] = {
    val collectiblesOfTypeEntityIds = collectibles.filter(_.typeVersionId === typeVersionId).map(_.entityId)
    db.run((for {
      _ <- properties.filter(_.parentId in collectiblesOfTypeEntityIds).delete
      _ <- collectibles.filter(_.entityId in collectiblesOfTypeEntityIds).delete
      _ <- entities.filter(_.id in collectiblesOfTypeEntityIds).delete
      _ <- constraints.filter(_.typeVersionId === typeVersionId).delete
      _ <- typeVersions.filter(_.id === typeVersionId).delete
    } yield ()).transactionally)
  }

  /**
   * Delete a [[modules.subject.model.Collectible Collectible]] and all associated data from the db.
   *
   * @param collectible the Collectible to delete
   * @return Future[Unit]
   */
  def delete(collectible: Collectible): Future[Unit] = {
    db.run((for {
      _ <- properties.filter(_.parentId === collectible.entityId).delete
      _ <- properties.filter(_.parentId === collectible.entityId).delete
      _ <- collectibles.filter(_.id === collectible.id).delete
      _ <- entities.filter(_.id === collectible.entityId).delete
    } yield ()).transactionally)
  }

  /**
   * Get the [[modules.subject.model.Collectible Collectible]] of given id.
   * <p> The [[modules.subject.model.ExtendedCollectible ExtendedCollectible]] with all [[modules.core.model.Property Properties]]
   * and [[modules.core.model.Viewer Viewers]] is returned.
   *
   * @param id of the collectible
   * @return Future Option[ExtendedCollectible]
   */
  def getExtendedCollectible(id: Long): Future[Option[ExtendedCollectible]] = {

    val collectibleQuery = collectibles.filter(_.id === id)

    val viewerQuery = collectibleQuery join collections on (_.collectionId === _.id) join
      viewers on (_._2.entityId === _.targetId) join
      groups on (_._2.viewerId === _.id)

    val propertyQuery = collectibleQuery joinLeft properties on (_.entityId === _.parentId)

    val typeQuery = collectibleQuery join typeVersions on (_.typeVersionId === _.id) join entityTypes on (_._2.typeId === _.id)

    for {
      viewerResult <- db.run(viewerQuery.result)
      propertyResult <- db.run(propertyQuery.result)
      typeResult <- db.run(typeQuery.result)
    } yield {
      val collectibleWithProperties = propertyResult.groupBy(_._1).mapValues(values => values.map(_._2)).headOption
      if (collectibleWithProperties.isEmpty) {
        None
      } else {
        val collectible = collectibleWithProperties.get._1
        //Note: only viewers of that particular collectible (parent) are in the result
        val viewerRelations = viewerResult.map(value => (value._2, value._1._2))
        val viewerCombinator = ViewerCombinator.fromRelations(viewerRelations)
        val entityType = typeResult.head._2

        Some(ExtendedCollectible(collectible, collectibleWithProperties.get._2.map(_.get), viewerCombinator, entityType))
      }
    }
  }

  /**
   * Update the state attribute of a [[modules.subject.model.Collectible Collectible]].
   *
   * @param collectibleId id of the collectible.
   * @param newState      new [[modules.subject.model.SubjectState]] value
   * @return Future[Int]
   */
  def updateState(collectibleId: Long, newState: SubjectState.State): Future[Int] = {
    db.run(collectibles.filter(_.id === collectibleId).map(_.state).update(newState.toString))
  }

  /**
   * Add new [[modules.core.model.Constraint Constraints]] to a [[modules.core.model.FlimeyEntity FlimeyEntity]] of
   * the [[modules.subject.model.Collectible Collectible]] subtype.
   * <p> The id of all new Constraints must be set to 0 to enable auto increment.
   * <p> This method makes a difference between new propertyConstraints (Constraints of the HasProperty type) and other
   * Constraints.
   * <p> <strong>If you add new HasProperty Constraints the wrong way (via otherConstraints or just
   * [[modules.core.repository.ConstraintRepository#addConstraint]]) will lead to loosing the integrity of the type
   * system. </strong>
   *
   * @param typeVersionId       id of the [[modules.core.model.TypeVersion TypeVersion]] (of a Collectible) to add the new Constraints to.
   * @param propertyConstraints new Constraints of HasProperty type
   * @param otherConstraints    new Constraints NOT of HasProperty type
   * @return Future[Unit]
   */
  def addConstraints(typeVersionId: Long, propertyConstraints: Seq[Constraint], otherConstraints: Seq[Constraint]): Future[Unit] = {

    val allConstraints = (propertyConstraints ++ otherConstraints) map (c => Constraint(c.id, c.c, c.v1, c.v2, c.byPlugin, typeVersionId))

    db.run((for {
      entityIDsWithType <- collectibles.filter(_.typeVersionId === typeVersionId).map(_.entityId).result
      _ <- DBIO.sequence(propertyConstraints.map(propertyConstraint => {
        properties ++= entityIDsWithType.map(entityId => Property(0, propertyConstraint.v1, "", entityId))
      }))
      _ <- (constraints returning constraints.map(_.id)) ++= allConstraints
    } yield ()).transactionally)
  }

  /**
   * Delete [[modules.core.model.Constraint Constraints]] of a [[modules.subject.model.Collection Collectible]] associated
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

    val entityIDsWithType = collectibles.filter(_.typeVersionId === typeVersionId).map(_.entityId)

    db.run((for {
      _ <- properties.filter(_.parentId in entityIDsWithType).filter(_.key.inSet(deletedPropertyKeys)).delete
      _ <- constraints.filter(_.id.inSet(deletedConstraintIds)).delete
    } yield ()).transactionally)
  }

}