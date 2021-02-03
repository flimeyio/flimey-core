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

package modules.core.repository

import com.google.inject.Inject
import modules.core.model.{Property, Viewer}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for abstract [[modules.core.model.FlimeyEntity FlimeyEntity]] handling.
 * <p> Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class FlimeyEntityRepository @Inject()(@NamedDatabase("flimey_data") protected val dbConfigProvider: DatabaseConfigProvider)(
  implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val viewers = TableQuery[ViewerTable]
  val properties = TableQuery[PropertyTable]

  /**
   * Update FlimeyEntity properties and Viewers.
   * <p> Updates the value field of all given Properties.
   * <p> Deletes all given deleted Viewers.
   * <p> Inserts all given new Viewers. The new Viewer objects must be complete and must already contain the target id.
   *
   * @param propertiesUpdate Properties to update the value field
   * @param deletedViewers   Group ids of Viewers to delete
   * @param newViewers       Viewers to add - id must be 0
   * @return Future[Unit]
   **/
  def update(propertiesUpdate: Seq[Property], deletedViewers: Set[Long], newViewers: Set[Viewer]): Future[Unit] = {
    db.run((for {
      _ <- DBIO.sequence(propertiesUpdate.map(update => {
        properties.filter(_.id === update.id).map(_.value).update(update.value)
      }))
      _ <- viewers.filter(_.viewerId.inSet(deletedViewers)).delete
      _ <- viewers ++= newViewers
    } yield ()).transactionally)
  }

}
