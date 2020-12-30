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

package user.repository

import com.google.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import user.model.{Group, Viewer}

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for Group - Viewer relations.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class GroupViewerRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val groupViewers = TableQuery[GroupViewerTable]
  val groups = TableQuery[GroupTable]

  /**
   * Insert a new Viewer.
   * Duplicated Viewer relations are not inserted and lead to an exception.
   *
   * @param viewer new viewer objecte with 0 id
   * @return Viewer id
   */
  def add(viewer: Viewer): Future[Long] = {
    db.run((groupViewers returning groupViewers.map(_.id)) += viewer)
  }

  /**
   * Delete a Viewer by its targetId - viewerId combination.
   *
   * @param targetId id of the target Group
   * @param viewerId id of the viewing Group
   * @return Future[Int]
   */
  def delete(targetId: Long, viewerId: Long): Future[Int] = {
    db.run(groupViewers.filter(_.targetId === targetId).filter(_.viewerId === viewerId).delete)
  }

  /**
   * Get all first class viewing Groups of a target Group.
   *
   * @param groupId id of the target Group
   * @return Seq[(Viewing Group, Viewer Rights)]
   */
  def getFirstClassViewers(groupId: Long): Future[Seq[(Group, Viewer)]] = {
    db.run((for {
      (c, s) <- groups.filter(_.id === groupId) join (groupViewers join groups on (_.viewerId === _.id)) on (_.id === _._1.targetId)
    } yield (c, s)).result).map(res => {
      res.map(data => (data._1, data._2._1))
    })
  }

  /**
   * Get all first class target Groups of a viewing Group.
   *
   * @param groupId id of the viewing Group
   * @return Seq[(Target Group, Viewer Rights)]
   */
  def getFirstClassTargets(groupId: Long): Future[Seq[(Group, Viewer)]] = {
    db.run((for {
      (c, s) <- (groups join groupViewers on (_.id === _.targetId)) join groups.filter(_.id === groupId) on (_._2.viewerId === _.id)
    } yield (c, s)).result).map(res => {
      res.map(_._1)
    })
  }

}
