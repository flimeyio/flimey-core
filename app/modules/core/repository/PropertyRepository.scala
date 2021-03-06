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
import modules.core.model.Property
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB Interface for Properties.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected database config
 * @param executionContext future execution context
 */
class PropertyRepository @Inject()(@NamedDatabase("flimey_data") protected val dbConfigProvider: DatabaseConfigProvider)(
  implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val properties = TableQuery[PropertyTable]

  /**
   * Add a new Property to the db.
   * The id must be set to 0 to enable auto increment.
   *
   * @param property new Property entity
   * @return new id future
   */
  def add(property: Property): Future[Long] = {
    db.run((properties returning properties.map(_.id)) += property)
  }

  /**
   * Delete a Property.
   *
   * @param id of the Property
   * @return result future
   */
  def delete(id: Long): Future[Int] = {
    db.run(properties.filter(_.id === id).delete)
  }

  /**
   * Get the sequence of all Properties associated to a particular Entity.
   *
   * @param entityId id of the Entity
   * @return future of Properties of the given Entity
   **/
  def getAssociated(entityId: Long): Future[Seq[Property]] = {
    db.run(properties.filter(_.parentId === entityId).sortBy(_.id).result)
  }

}