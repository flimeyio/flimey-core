/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020-2021 Karl Kegel
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

package modules.core.repository

import com.google.inject.Inject
import modules.asset.repository.AssetRepository
import modules.core.model._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for EntityTypes.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class TypeRepository @Inject()(@NamedDatabase("flimey_data") protected val dbConfigProvider: DatabaseConfigProvider,
                               val assetRepository: AssetRepository)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val types = TableQuery[TypeTable]
  val typeVersions = TableQuery[TypeVersionTable]
  val constraints = TableQuery[ConstraintTable]
  val properties = TableQuery[PropertyTable]

  /**
   * Add a new [[modules.core.model.EntityType EntityType]] to the db.
   * <p> The id must be set to 0 to enable auto increment.
   * <p> This adds also a new [[modules.core.model.TypeVersion TypeVersion]] with version number 0.
   *
   * @param entityType new EntityType entity
   * @return new id future
   */
  def add(entityType: EntityType): Future[Long] = {
    db.run((for {
      newTypeId <- (types returning types.map(_.id)) += entityType
      _ <- (typeVersions returning typeVersions.map(_.id)) += TypeVersion(0, newTypeId, 1)
    } yield newTypeId).transactionally)
  }

  def addVersion(typeVersion: TypeVersion, newConstraints: Seq[Constraint]): Future[Long] = {
    db.run((for {
      typeVersionId <- (typeVersions returning typeVersions.map(_.id)) += typeVersion
      _ <- constraints ++= newConstraints.map(c => Constraint(0, c.c, c.v1, c.v2, c.byPlugin, typeVersionId))
    } yield typeVersionId).transactionally)
  }

  /**
   * Update an existing EntityType
   *
   * @param entityType to update
   * @return future
   */
  def update(entityType: EntityType): Future[Int] = {
    db.run(types.filter(_.id === entityType.id).map(a => (a.value, a.active)).update((entityType.value, entityType.active)))
  }

  /**
   * Get an EntityType by its id.
   *
   * @param id          of the EntityType
   * @param derivesFrom optional parent type specification
   * @return Future Option[EntityType]
   */
  def get(id: Long, derivesFrom: Option[String] = None): Future[Option[EntityType]] = {
    if (derivesFrom.isEmpty) {
      db.run(types.filter(_.id === id).result.headOption)
    } else {
      db.run(types.filter(_.id === id).filter(_.typeOf === derivesFrom.get).result.headOption)
    }
  }

  /**
   * Get a [[modules.core.model.VersionedEntityType VersionedEntityType]] by its id.
   *
   * @param typeVersionId of the [[modules.core.model.TypeVersion TypeVersion]]
   * @param derivesFrom   optional parent type specification
   * @return Future Option[VersionedEntityType]
   */
  def getVersioned(typeVersionId: Long, derivesFrom: Option[String] = None): Future[Option[VersionedEntityType]] = {
    var query = typeVersions.filter(_.id === typeVersionId) join types on (_.typeId === _.id)
    if (derivesFrom.isDefined) {
      query = typeVersions.filter(_.id === typeVersionId) join types.filter(_.typeOf === derivesFrom.get) on (_.typeId === _.id)
    }
    db.run(query.result).map(res => res.map(value => VersionedEntityType(value._2, value._1)).headOption)
  }

  /**
   * Get all EntityTypes.
   *
   * @param derivesFrom optional parent type specification
   * @return Future Seq[EntityType]
   */
  def getAll(derivesFrom: Option[String] = None): Future[Seq[EntityType]] = {
    if (derivesFrom.isEmpty) {
      db.run(types.sortBy(_.id.asc).result)
    } else {
      db.run(types.filter(_.typeOf === derivesFrom.get).sortBy(_.id.asc).result)
    }
  }

  /**
   * Get all [[modules.core.model.VersionedEntityType VersionedEntityTypes]].
   *
   * @param derivesFrom optional parent type specification
   * @return Future Seq[VersionedEntityType]
   */
  def getAllVersioned(derivesFrom: Option[String] = None): Future[Seq[VersionedEntityType]] = {
    var query = typeVersions join types on (_.typeId === _.id)
    if (derivesFrom.isDefined) {
      query = typeVersions join types.filter(_.typeOf === derivesFrom.get) on (_.typeId === _.id)
    }
    db.run(query.result).map(res => res.map(value => VersionedEntityType(value._2, value._1)).sortBy(_.entityType.id))
  }

  /**
   * Get all [[modules.core.model.ExtendedEntityType ExtendedEntityTypes]] defined by their values (names).
   *
   * @param values names of the EntityTypes to fetch
   * @return Future Seq[ExtendedEntityType]
   */
  def getAllExtended(values: Seq[String]): Future[Seq[ExtendedEntityType]] = {
    val query = typeVersions join
      types.filter(_.value inSet values) on (_.typeId === _.id) joinLeft
      constraints on (_._1.id === _.typeVersionId)
    db.run(query.result).map(res => {
      res.groupBy(_._1).mapValues(v => v.filter(_._2.isDefined).map(_._2.get)).map(typeWithConstraints =>
        ExtendedEntityType(typeWithConstraints._1._2, typeWithConstraints._1._1, typeWithConstraints._2)).toSeq
    })
  }

  /**
   * Get all [[modules.core.model.ExtendedEntityType ExtendedEntityTypes]].
   *
   * @param derivesFrom optional parent type specification
   * @return Future Seq[ExtendedEntityType]
   */
  def getAllExtended(derivesFrom: Option[String] = None): Future[Seq[ExtendedEntityType]] = {
    var query = typeVersions join types on (_.typeId === _.id) joinLeft constraints on (_._1.id === _.typeVersionId)
    if (derivesFrom.isDefined) {
      query = typeVersions join types.filter(_.typeOf === derivesFrom.get) on (_.typeId === _.id) joinLeft
        constraints on (_._1.id === _.typeVersionId)
    }
    db.run((for {
      (c, s) <- query
    } yield (c, s)).result) map (res => {
      res.groupBy(_._1).mapValues(v => v.filter(_._2.isDefined).map(_._2.get)).map(typeWithConstraints =>
        ExtendedEntityType(typeWithConstraints._1._2, typeWithConstraints._1._1, typeWithConstraints._2)).toSeq
    })
  }

  /**
   * Get all [[modules.core.model.ExtendedEntityType ExtendedEntityTypes]] (Versions) of a specified EntityType.
   *
   * @param typeId      id of the parent [[modules.core.model.EntityType EntityType]]
   * @param derivesFrom optional parent type specification
   * @return Future Seq[ExtendedEntityType]
   */
  def getAllExtendedVersions(typeId: Long, derivesFrom: Option[String] = None): Future[Seq[ExtendedEntityType]] = {
    var query = typeVersions join types.filter(_.id === typeId) on (_.typeId === _.id) joinLeft constraints on (_._1.id === _.typeVersionId)
    if (derivesFrom.isDefined) {
      query = typeVersions join types.filter(_.id === typeId).filter(_.typeOf === derivesFrom.get) on (_.typeId === _.id) joinLeft
        constraints on (_._1.id === _.typeVersionId)
    }
    db.run((for {
      (c, s) <- query
    } yield (c, s)).result) map (res => {
      res.groupBy(_._1).mapValues(v => v.filter(_._2.isDefined).map(_._2.get)).map(typeWithConstraints =>
        ExtendedEntityType(typeWithConstraints._1._2, typeWithConstraints._1._1, typeWithConstraints._2)).toSeq
    })
  }

  /**
   * Get an [[modules.core.model.ExtendedEntityType ExtendedEntityType]].
   *
   * @param typeVersionId of the [[modules.core.model.TypeVersion TypeVersion]]
   * @param derivesFrom   optional parent type specification
   * @return Future Option[ExtendedEntityType]
   */
  def getExtended(typeVersionId: Long, derivesFrom: Option[String] = None): Future[Option[ExtendedEntityType]] = {
    var query = typeVersions.filter(_.id === typeVersionId) join types on (_.typeId === _.id) joinLeft
      constraints on (_._1.id === _.typeVersionId)
    if (derivesFrom.isDefined) {
      query = typeVersions.filter(_.id === typeVersionId) join types.filter(_.typeOf === derivesFrom.get) on (_.typeId === _.id) joinLeft
        constraints on (_._1.id === _.typeVersionId)
    }
    db.run((for {
      (c, s) <- query
    } yield (c, s)).result).map(res => {
      res.groupBy(_._1).mapValues(v => v.filter(_._2.isDefined).map(_._2.get)).map(typeWithConstraints =>
        ExtendedEntityType(typeWithConstraints._1._2, typeWithConstraints._1._1, typeWithConstraints._2)).headOption
    })
  }

}