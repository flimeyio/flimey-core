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

package db.asset

import com.google.inject.Inject
import model.asset.Asset
import model.generic.Property
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for Assets.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class AssetRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val assets = TableQuery[AssetTable]
  val assetTypes = TableQuery[AssetTypeTable]
  val assetConstraints = TableQuery[AssetConstraintTable]
  val assetProperties = TableQuery[AssetPropertyTable]

  /**
   * Add a new Asset with Properties to the db.
   * The id must be set to 0 to enable auto increment.
   *
   * @param asset new Asset entity
   * @return new id future
   */
  def add(asset: Asset, properties: Seq[Property]): Future[Unit] = {
    db.run((for {
      key <- (assets returning assets.map(_.id)) += asset
      _ <- assetProperties ++= properties.map(p => Property(0, p.key, p.value, key))
    } yield ()).transactionally)
  }

  /**
   * Delete an Asset.
   * This operation will also delete all Properties
   *
   * @param id of the Asset to delete
   * @return future
   */
  def delete(id: Long): Future[Unit] = {
    db.run((for {
      _ <- assetProperties.filter(_.parentId === id).delete
      _ <- assets.filter(_.typeId === id).delete
    } yield ()).transactionally)
  }

  /**
   * Get the all Assets with Properties of a specific AssetType.
   * This operation is more performant then fetching the data by separate methods.
   * <br />
   * It can be expected that the Property seq contains either a single None Option or only defined Properties.
   *
   * @return future of result Seq (Properties mapped to Asset)
   */
  def getAll(typeId: Long): Future[Seq[(Asset, Seq[Option[Property]])]] = {
    db.run((for {
      (c, s) <- assets.filter(_.typeId === typeId).sortBy(_.id) joinLeft assetProperties.sortBy(_.id) on (_.id === _.parentId)
    } yield (c, s)).result).map(res => {
      res.groupBy(_._1.id).mapValues(values => (values.map(_._1).headOption, values.map(_._2))).values.toSeq.filter(_._1.isDefined).map(t => (t._1.get, t._2))
    })
  }

  /**
   * Get an Asset with its Properties by ID.
   * This operation is more performant then fetching the data by separate methods.
   * <br />
   * It can be expected that the Property seq contains either a single None Option or only defined Properties.
   *
   * @param id of the Asset
   * @return future of result (Properties mapped to Asset)
   */
  def get(id: Long): Future[(Option[Asset], Seq[Option[Property]])] = {
    db.run((for {
      (c, s) <- assets.filter(_.id === id) joinLeft assetProperties.sortBy(_.id) on (_.id === _.parentId)
    } yield (c, s)).result).map(res => {
      if (res.isEmpty) {
        (None, Seq())
      } else {
        res.groupBy(_._1.id).mapValues(values => (values.map(_._1).headOption, values.map(_._2))).values.head
      }
    })
  }

}