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
import model.generic.Property
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB Interface for (Asset)Properties.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected database config
 * @param executionContext future execution context
 */
class AssetPropertyRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val assetProperties = TableQuery[AssetPropertyTable]

  /**
   * Add a new (Asset)Property to the db.
   * The id must be set to 0 to enable auto increment.
   *
   * @param property new Property entity
   * @return new id future
   */
  def add(property: Property): Future[Long] = {
    db.run((assetProperties returning assetProperties.map(_.id)) += property)
  }

  /**
   * Delete a (Asset)Property.
   *
   * @param id of the Property
   * @return result future
   */
  def delete(id: Long): Future[Int] = {
    db.run(assetProperties.filter(_.id === id).delete)
  }

  /**
   * Get the sequence of all Properties associated to a particular Asset.
   *
   * @param assetId id of the Asset
   * @return future of Properties of the given Asset
   * */
  def getAssociated(assetId: Long): Future[Seq[Property]] = {
    db.run(assetProperties.filter(_.parentId === assetId).sortBy(_.id).result)
  }

  /**
   * Update a sequence of Properties at once.
   * Only the 'value' field is updated.
   *
   * @param properties to update
   * @return result future sequence
   */
  def update(properties: Seq[Property]): Future[Seq[Int]] = {
    val acc = DBIO.sequence(properties.map(update => {
      assetProperties.filter(_.id === update.id).map(_.value).update(update.value)
    }))
    db.run(acc)
  }

}