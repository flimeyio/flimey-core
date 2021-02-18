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

package modules.news.repository

import java.sql.Timestamp

import com.google.inject.Inject
import modules.news.model.{ExtendedNewsEvent, NewsEvent, NewsTarget}
import modules.user.model.Group
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

/**
 * Repository mapping for managing [[modules.news.model.NewsEvent NewsEvents]].
 *
 * @param dbConfigProvider injectedDatabaseConfigProvider
 * @param executionContext injected ExecutionContext
 */
class NewsEventRepository @Inject()(@NamedDatabase("flimey_data") protected val dbConfigProvider: DatabaseConfigProvider)(
  implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val newsEvents = TableQuery[NewsEventTable]
  val newsTargets = TableQuery[NewsTargetTable]

  /**
   * Add a new [[modules.news.model.NewsEvent NewsEvent]] together with its viewers ([[modules.news.model.NewsTarget NewsTargets]])
   * to the repository.
   * <p> The id of the newsEvent must be set to 0 to enable auto increment.
   *
   * @param newsEvent new NewsEvent to save
   * @param viewers all [[modules.user.model.Group Groups]] which can see the NewsEvent
   * @return Future[Unit]
   */
  def addNewsEvent(newsEvent: NewsEvent, viewers: Set[Group]): Future[Unit] = {
    db.run((for {
      newsId <- (newsEvents returning newsEvents.map(_.id)) += newsEvent
      _ <- newsTargets ++= viewers.map(group => NewsTarget(0, newsId, group.id))
    } yield ()).transactionally)
  }

  /**
   * Get a number of [[modules.news.model.ExtendedNewsEvent ExtendedNewsEvents]] specified by a set of
   * [[modules.user.model.Group Groups]] which can see them.
   *
   * @param viewers Groups of which at least one must be targeted by a [[modules.news.model.NewsEvent NewsEvent]]
   * @param maxCount number of ExtendedNewsEvents to retrieve. The sequence is sorted reverse chronologically
   * @return Future Seq[ExtendedNewsEvent]
   */
  def getNewsEvents(viewers: Set[Group], maxCount: Long): Future[Seq[ExtendedNewsEvent]] = {
    val viewerIds = viewers.map(_.id)
    val accessableNewsEventIds = newsTargets.filter(_.groupId inSet viewerIds).groupBy(_.newsId).map (_._1).sortBy(_.asc).take(maxCount)
    db.run(newsEvents.filter(_.id in accessableNewsEventIds) join newsTargets.filter(_.groupId inSet viewerIds) on (_.id === _.newsId) result).map(res => {
      val newsWithViewers = res.groupBy(_._1).mapValues(values => values.map(_._2.groupId))
      newsWithViewers.keys.map(newsEvent => {
        ExtendedNewsEvent(
          newsEvent,
          newsWithViewers(newsEvent).map(groupId => viewers.find(_.id == groupId).get)
        )
      }).toSeq.sortBy(-_.newsEvent.id)
    })
  }

  /**
   * Delete all [[modules.news.model.NewsEvent NewsEvents]] together with their [[modules.news.model.NewsTarget NewsTargets]]
   * which have a date older than the specified minTimestamp.
   *
   * @param minTimestamp minimum date a NewsEvent must have to not be deleted
   * @return Future[Unit]
   */
  def deleteOlderThan(minTimestamp: Timestamp): Future[Unit] = {
    val newsEventsToDeleteIds = newsEvents.filter(_.date < minTimestamp).map(_.id)
    db.run((for {
      _ <- newsTargets.filter(_.newsId in newsEventsToDeleteIds).delete
      - <- newsEvents.filter(_.id in newsEventsToDeleteIds).delete
    } yield ()).transactionally)
  }

}
