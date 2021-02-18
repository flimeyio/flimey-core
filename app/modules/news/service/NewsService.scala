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

package modules.news.service

import java.sql.Timestamp

import com.google.inject.Inject
import modules.auth.model.Ticket
import modules.auth.util.RoleAssertion
import modules.news.model.{ExtendedNewsEvent, NewsEvent, NewsType}
import modules.news.repository.NewsEventRepository
import modules.user.model.Group
import play.api.{Logger, Logging}

import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic to work with [[modules.news.model.NewsEvent NewsEvents]].
 * <p>  This class is normally used by dependency injection inside controller classes.
 *
 * @param newsEventRepository injected [[modules.news.repository.NewsEventRepository NewsEventRepository]]
 */
class NewsService @Inject()(newsEventRepository: NewsEventRepository) extends NewsFactory with Logging {

  /**
   * Add a new [[modules.news.model.NewsEvent NewsEvent]].
   * <p> <strong> If this operation fails, it will dump the exception and pretend to have finished successfully.</strong>
   * <p> Requires at least WORKER rights.
   * <p> This is a safe implementation and can be used inside controller classes.
   *
   * @param newsEvent new NewsEvent
   * @param viewers   all [[modules.user.model.Group Groups]] which can see this NewsEvent
   * @param ticket    implicit authentication ticket
   * @return Future[Unit]
   */
  def addEvent(newsEvent: NewsEvent, viewers: Set[Group])(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertWorker
      newsEventRepository.addNewsEvent(newsEvent, viewers)
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage)
        Future.successful()
    }
  }

  /**
   * Add a new [[modules.news.model.NewsEvent NewsEvent]] triggered by a [[modules.subject.model.Collection Collection]].
   * <p> Requires at least WORKER rights.
   * <p> This is a safe implementation and can be used inside controller classes.
   *
   * @see [[modules.news.service.NewsService#addEvent]]
   * @param collectionId id of the Collection
   * @param newsType     [[modules.news.model.NewsType NewsType]] which triggered the NewsEvent
   * @param viewers      all [[modules.user.model.Group Groups]] which can see this NewsEvent
   * @param ticket       implicit authentication ticket
   * @return Future[Unit]
   */
  def addCollectionEvent(collectionId: Long, newsType: NewsType.Value, viewers: Set[Group])(implicit ticket: Ticket): Future[Unit] = {
    addEvent(eventFrom(collectionId, newsType), viewers)
  }

  /**
   * Add a new [[modules.news.model.NewsEvent NewsEvent]] triggered by a [[modules.subject.model.Collectible Collectible]].
   * <p> Requires at least WORKER rights.
   * <p> This is a safe implementation and can be used inside controller classes.
   *
   * @see [[modules.news.service.NewsService#addEvent]]
   * @param collectionId  id of the parent [[modules.subject.model.Collection Collection]]
   * @param collectibleId id of the Collectible
   * @param newsType      [[modules.news.model.NewsType NewsType]] which triggered the NewsEvent
   * @param viewers       all [[modules.user.model.Group Groups]] which can see this NewsEvent
   * @param ticket        implicit authentication ticket
   * @return Future[Unit]
   */
  def addCollectibleEvent(collectionId: Long, collectibleId: Long, newsType: NewsType.Value, viewers: Set[Group])(implicit ticket: Ticket): Future[Unit] = {
    addEvent(eventFrom(collectionId, collectibleId, newsType), viewers)
  }

  /**
   * Get the overview feed consisting of a configurable number of the latest [[modules.news.model.NewsEvent NewsEvents]].
   * <p> The news are wrapped inside [[modules.news.model.ExtendedNewsEvent ExtendedNewsEvents]].
   * <p> The news a [[modules.user.model.User User]] can access is defined by his ticket.
   * <p> Requires at least WORKER rights.
   * <p> This is a safe implementation and can be used inside controller classes.
   *
   * @param maxCount maximum number of returned NewsEvents, but can always be less
   * @param ticket implicit authentication ticket
   * @return Future Seq[ExtendedNewsEvent]
   */
  def getFeed(maxCount: Long)(implicit ticket: Ticket): Future[Seq[ExtendedNewsEvent]] = {
    try {
      RoleAssertion.assertWorker
      newsEventRepository.getNewsEvents(ticket.accessRights.getAllViewingGroups, maxCount)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete all [[modules.news.model.NewsEvent NewsEvents]] which are older than the specified feedBase.
   * <p> <strong> This method must only be used by a cron-job which cleans the news repository in a fixed interval.
   * Must not be called by controller actions! </strong>
   * <p> If this operation fails, it will dump the exception and pretend to have finished successfully.
   *
   * @param feedBase minimum timestamp a NewsEvent must have not to be deleted
   * @return Future[Unit]
   */
  def clean(feedBase: Timestamp): Future[Unit] = {
    try {
      newsEventRepository.deleteOlderThan(feedBase)
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage)
        Future.successful()
    }
  }

}
