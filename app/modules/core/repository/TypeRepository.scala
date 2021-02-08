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
import modules.core.model.{Constraint, EntityType, ExtendedEntityType}
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
  val constraints = TableQuery[ConstraintTable]
  val properties = TableQuery[PropertyTable]

  /**
   * Add a new EntityType to the db.
   * The id must be set to 0 to enable auto increment.
   *
   * @param entityType new EntityType entity
   * @return new id future
   */
  def add(entityType: EntityType): Future[Long] = {
    db.run((types returning types.map(_.id)) += entityType)
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
   * @return future of EntityType if found, else None
   */
  def get(id: Long, derivesFrom: Option[String] = None): Future[Option[EntityType]] = {
    if (derivesFrom.isEmpty) {
      db.run(types.filter(_.id === id).result.headOption)
    } else {
      db.run(types.filter(_.id === id).filter(_.typeOf === derivesFrom.get).result.headOption)
    }
  }

  /**
   * Get all EntityTypes.
   *
   * @param derivesFrom optional parent type specification
   * @return future of result
   */
  def getAll(derivesFrom: Option[String] = None): Future[Seq[EntityType]] = {
    if (derivesFrom.isEmpty) {
      db.run(types.sortBy(_.id.asc).result)
    } else {
      db.run(types.filter(_.typeOf === derivesFrom.get).sortBy(_.id.asc).result)
    }
  }

  /**
   * Get all [[modules.core.model.ExtendedEntityType ExtendedEntityTypes]] defined by their values (names).
   *
   * @param values names of the EntityTypes to fetch
   * @return Future Seq[ExtendedEntityType]
   */
  def getAllExtended(values: Seq[String]): Future[Seq[ExtendedEntityType]] = {
    val query = types.filter(_.value inSet values) joinLeft constraints on (_.id === _.typeId)
    db.run(query.result).map(res => {
      res.groupBy(_._1).mapValues(v => v.filter(_._2.isDefined).map(_._2.get)).map(typeWithConstraints =>
        ExtendedEntityType(typeWithConstraints._1, typeWithConstraints._2)).toSeq
    })
  }

  /**
   * Get all [[modules.core.model.ExtendedEntityType ExtendedEntityTypes]].
   *
   * @param derivesFrom optional parent type specification
   * @return Future Seq[ExtendedEntityType]
   */
  def getAllExtended(derivesFrom: Option[String] = None): Future[Seq[ExtendedEntityType]] = {
    var query = types joinLeft constraints on (_.id === _.typeId)
    if (derivesFrom.isDefined) {
      query = types.filter(_.typeOf === derivesFrom.get) joinLeft constraints on (_.id === _.typeId)
    }
    db.run((for {
      (c, s) <- query
    } yield (c, s)).result) map (res => {
      res.groupBy(_._1).mapValues(v => v.filter(_._2.isDefined).map(_._2.get)).map(typeWithConstraints =>
        ExtendedEntityType(typeWithConstraints._1, typeWithConstraints._2)).toSeq
    })
  }

  /**
   * Get an EntityType with all its Constraints if it has an existing model.
   * This operation is more performant then fetching both separately.
   *
   * @param id          of the EntityType
   * @param derivesFrom optional parent type specification
   * @return result future with pair of AssetType and Constraints
   */
  def getComplete(id: Long, derivesFrom: Option[String] = None): Future[(Option[EntityType], Seq[Constraint])] = {
    var query = types.filter(_.id === id) joinLeft constraints on (_.id === _.typeId)
    if (derivesFrom.isDefined) {
      query = types.filter(_.id === id).filter(_.typeOf === derivesFrom.get) joinLeft constraints on (_.id === _.typeId)
    }
    db.run((for {
      (c, s) <- query
    } yield (c, s)).result) map (res => {
      if (res.isEmpty) {
        (None, Seq())
      } else {
        val result = res.groupBy(_._1.id).mapValues(values => (values.map(_._1).headOption, values.map(_._2))).values.head
        (result._1, result._2.filter(_.isDefined).map(_.get).sortBy(_.id))
      }
    })
  }

}