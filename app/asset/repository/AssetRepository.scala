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

import asset.model.{Asset, AssetProperty, ExtendedAsset}
import com.google.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import user.model.{Group, Viewer, ViewerCombinator}
import user.repository.GroupTable

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
  val assetViewers = TableQuery[AssetViewerTable]
  val groups = TableQuery[GroupTable]

  /**
   * Add a new Asset with Properties to the db.<br />
   * The Asset id and Property ids are set to 0 to enable auto increment.
   * <p> Only valid Asset configurations should be added to the repository.
   *
   * @param asset      new Asset entity
   * @param properties AssetProperties of the Asset.
   * @param viewers    Viewers of the Asset.
   * @return Future[Unit]
   */
  def add(asset: Asset, properties: Seq[AssetProperty], viewers: Seq[Viewer]): Future[Unit] = {
    db.run((for {
      key <- (assets returning assets.map(_.id)) += asset
      _ <- assetProperties ++= properties.map(p => AssetProperty(0, p.key, p.value, key))
      _ <- assetViewers ++= viewers.map(v => Viewer(0, key, v.viewerId, v.role))
    } yield ()).transactionally)
  }

  /**
   * Update Asset properties and Viewers.
   * <p> Updates the value field of all given AssetProperties.
   * <p> Deletes all given deleted Viewers.
   * <p> Inserts all given new Viewers. The new Viewer objects must be complete and must already contain the target id.
   *
   * @param properties Properties to update the value field
   * @param deletedViewers Group ids of Viewers to delete
   * @param newViewers Viewers to add - id must be 0
   * @return Future[Unit]
   **/
  def update(properties: Seq[AssetProperty], deletedViewers: Set[Long], newViewers: Set[Viewer]): Future[Unit] = {
    db.run((for {
      _ <- DBIO.sequence(properties.map(update => {
        assetProperties.filter(_.id === update.id).map(_.value).update(update.value)
      }))
      _ <- assetViewers.filter(_.viewerId.inSet(deletedViewers)).delete
      _ <- assetViewers ++= newViewers
    } yield ()).transactionally)
  }

  /**
   * Delete an Asset.<br />
   * This operation will also delete all Properties
   *
   * @param id of the Asset to delete
   * @return future
   */
  def delete(id: Long): Future[Unit] = {
    db.run((for {
      _ <- assetProperties.filter(_.parentId === id).delete
      _ <- assetViewers.filter(_.targetId === id).delete
      _ <- assets.filter(_.id === id).delete
    } yield ()).transactionally)
  }

  /**
   * Get an Asset with its Properties by ID.<br />
   * <p> Only Assets which are part of the specified Groups can be fetched.
   *
   * @param id       of the Asset
   * @param groupIds ids of Groups which must be able to view the Asset
   * @return Future Option[ExtendedAsset]
   */
  def get(id: Long, groupIds: Set[Long]): Future[Option[ExtendedAsset]] = {
    db.run((for {
      (c, s) <- ((assets.filter(_.id === id) join assetProperties.sortBy(_.id) on (_.id === _.parentId)) join
        assetViewers.filter(_.viewerId.inSet(groupIds)) on (_._1.id === _.targetId)) join
        groups on (_._2.viewerId === _.id)
    } yield (c, s)).result).map(res => {
      if (res.isEmpty) {
        None
      } else {
        val assetData = res.groupBy(_._1._1._1).head
        val asset = assetData._1
        Option(extendedAssetFromRaw(asset, assetData._2))
      }
    })
  }

  /**
   * Get a number of Assets by multiple query parameters.<br />
   * <p> Only Assets which are part of the specified Groups can be fetched.
   *
   * @param groupIds ids of the Groups, of which at least one must have access to the Asset
   * @param typeId   id of the AssetType, the Asset must have
   * @param limit    maximum number of retrieved Assets - recommended to keep as small as possible
   * @param offset   number of Assets to skip
   * @return Future Seq[ExtendedAsset]
   */
  def getAssetSubset(groupIds: Set[Long], typeId: Long, limit: Int, offset: Int): Future[Seq[ExtendedAsset]] = {
    //build sub-query to get all asset ids of assets of the given type which can be accessed by the given groups
    //the returned keys are limited to provide the defined number of results for the second query
    val subQuery = (for {
      (c, s) <- assets.filter(_.typeId === typeId) join
        assetViewers.filter(_.viewerId.inSet(groupIds)) on (_.id === _.targetId)
    } yield (c, s)).groupBy(_._1.id).map(_._1)
    //FIXME basically, the limit should be applied here and not in the main query...
    // but MYSQL is not able to support that and throws a runtime error. Maybe try with Postgres again.

    //main query to fetch all data from the by the sub-query specified assets
    db.run((for {
      (c, s) <- ((assets.filter(_.id in subQuery).sortBy(_.id.desc).drop(offset).take(limit) join
        assetProperties on (_.id === _.parentId)) join
        assetViewers on (_._1.id === _.targetId)) join
        groups on (_._2.viewerId === _.id)
    } yield (c, s)).result).map(res => {
      val assets = res.groupBy(_._1._1._1)
      assets.keys.map(asset => {
        val parameters = assets(asset)
        extendedAssetFromRaw(asset, parameters)
      }).toSeq.sortBy(-_.asset.id)
    })
  }

  /**
   * Build and ExtendedAsset model object from raw query data (join table entries)
   * <p> This method does not perform any validation or verification!
   * <p> The given data rows must all be able to be grouped on a single Asset. This Assets values are processed.
   *
   * @param asset the Asset to build the ExtendedAsset from (base to group values)
   * @param parameters sequence of (partly redundant) table data after expected join operations
   * @return ExtendedAsset
   */
  private def extendedAssetFromRaw(asset: Asset, parameters: Seq[(((Asset, AssetProperty), Viewer), Group)]): ExtendedAsset = {
    val properties = parameters.groupBy(_._1._1._2).keySet.toSeq.sortBy(_.id)
    val viewers = parameters.map(param => (param._1._2, param._2)).groupBy(_._1).mapValues(pairs => pairs.map(_._2).head).toSeq
    val assetViewerCombinator = ViewerCombinator.fromRelations(viewers.map(_.swap))
    ExtendedAsset(asset, properties, assetViewerCombinator)
  }

}