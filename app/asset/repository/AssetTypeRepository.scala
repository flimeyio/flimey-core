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

package asset.repository

import asset.model.{AssetConstraint, AssetType}
import com.google.inject.Inject
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
class AssetTypeRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, val assetRepository: AssetRepository)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val assetTypes = TableQuery[AssetTypeTable]
  val assetConstraints = TableQuery[AssetConstraintTable]
  val assets = TableQuery[AssetTable]
  val assetProperties = TableQuery[AssetPropertyTable]

  /**
   * Add a new AssetType to the db.
   * The id must be set to 0 to enable auto increment.
   *
   * @param assetType new AssetType entity
   * @return new id future
   */
  def add(assetType: AssetType): Future[Long] = {
    db.run((assetTypes returning assetTypes.map(_.id)) += assetType)
  }

  /**
   * Delete an AssetType and all associated Constraints, Assets and their Properties.
   * <br /><br />
   * <b>This is obviously a highly destructive operation!</b>
   *
   * @param id of the AssetType to delete
   * @return future
   */
  def delete(id: Long): Future[Unit] = {
    db.run((for {
      _ <- assetProperties.filter(_.parentId in assets.filter(_.typeId === id).map(_.id)).delete
      _ <- assets.filter(_.typeId === id).delete
      _ <- assetConstraints.filter(_.typeId === id).delete
      _ <- assetTypes.filter(_.id === id).delete
    } yield ()).transactionally)
  }

  /**
   * Update an existing AssetType
   *
   * @param assetType to update
   * @return future
   */
  def update(assetType: AssetType): Future[Int] = {
    db.run(assetTypes.filter(_.id === assetType.id).map(a => (a.value, a.active)).update((assetType.value, assetType.active)))
  }

  /**
   * Get an AssetType by its id.
   *
   * @param id of the AssetType
   * @return future of AssetType if found, else None
   */
  def get(id: Long): Future[Option[AssetType]] = {
    db.run(assetTypes.filter(_.id === id).result.headOption)
  }

  /**
   * Get all AssetTypes.
   *
   * @return future of result
   */
  def getAll: Future[Seq[AssetType]] = {
    db.run(assetTypes.result)
  }

  /**
   * Get an AssetType with all its Constraints if it has an existing model.
   * This operation is more performant then fetching both separately.
   * <br />
   * This operation returns only a defined AssetType option, if it has existing Constraints.
   * If the Constraint seq is empty, the AssetType option will be none.
   *
   * @param id of the AssetType
   * @return result future with pair of AssetType and Constraints
   */
  def getComplete(id: Long): Future[(Option[AssetType], Seq[AssetConstraint])] = {
    db.run((for {
      (c, s) <- assetTypes.filter(_.id === id) joinLeft assetConstraints on (_.id === _.typeId)
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