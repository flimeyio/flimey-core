/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020  Karl Kegel
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
import modules.asset.repository.{AssetRepository, AssetTable}
import modules.core.model.{Constraint, EntityType}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for AssetTypes.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class TypeRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, val assetRepository: AssetRepository)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val types = TableQuery[TypeTable]
  val constraints = TableQuery[ConstraintTable]
  val assets = TableQuery[AssetTable]
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
   * Delete an EntityType and all associated Constraints, Assets and their Properties.
   * <br /><br />
   * <b>This is obviously a highly destructive operation!</b>
   *
   * @param id of the type to delete
   * @return future
   */
    //FIXME Viewers etc. must also be deleted!! (maybe move to asset repository)
  def delete(id: Long): Future[Unit] = {
    db.run((for {
      _ <- properties.filter(_.parentId in assets.filter(_.typeId === id).map(_.id)).delete
      _ <- assets.filter(_.typeId === id).delete
      _ <- constraints.filter(_.typeId === id).delete
      _ <- types.filter(_.id === id).delete
    } yield ()).transactionally)
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
   * @param id of the EntityType
   * @return future of EntityType if found, else None
   */
  def get(id: Long): Future[Option[EntityType]] = {
    db.run(types.filter(_.id === id).result.headOption)
  }

  /**
   * Get all EntityTypes.
   *
   * @return future of result
   */
  def getAll: Future[Seq[EntityType]] = {
    db.run(types.result)
  }

  /**
   * Get an EntityType with all its Constraints if it has an existing model.
   * This operation is more performant then fetching both separately.
   *
   * @param id of the EntityType
   * @return result future with pair of AssetType and Constraints
   */
  def getComplete(id: Long): Future[(Option[EntityType], Seq[Constraint])] = {
    db.run((for {
      (c, s) <- types.filter(_.id === id) joinLeft constraints on (_.id === _.typeId)
    } yield (c, s)).result) map (res => {
      if(res.isEmpty){
        (None, Seq())
      }else {
        val result = res.groupBy(_._1.id).mapValues(values => (values.map(_._1).headOption, values.map(_._2))).values.head
        (result._1, result._2.filter(_.isDefined).map(_.get).sortBy(_.id))
      }
    })
  }

}