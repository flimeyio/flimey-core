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
import java.time.Instant

import modules.news.model.{NewsEvent, NewsType}

trait NewsFactory {

  /**
   * Create a new [[modules.news.model.NewsEvent NewsEvent]] from a [[modules.subject.model.Collection Collection]].
   *
   * @param collectionId id of the Collection
   * @param newsType     [[modules.news.model.NewsType NewsType]] which triggered the NewsEvent
   * @param description  an optional short description
   * @return NewsEvent
   */
  def eventFrom(collectionId: Long, newsType: NewsType.Value, description: Option[String]): NewsEvent = {
    NewsEvent(0, newsType, 1, description.getOrElse(""), NewsRouter.buildRoute(collectionId, newsType), Timestamp.from(Instant.now()))
  }

  /**
   * Create a new [[modules.news.model.NewsEvent NewsEvent]] from a [[modules.subject.model.Collectible Collectible]].
   *
   * @param collectionId  id of the parent [[modules.subject.model.Collection Collection]]
   * @param collectibleId id of the Collectible
   * @param newsType      [[modules.news.model.NewsType NewsType]] which triggered the NewsEvent
   * @param description   an optional short description
   * @return
   */
  def eventFrom(collectionId: Long, collectibleId: Long, newsType: NewsType.Value, description: Option[String]): NewsEvent = {
    NewsEvent(0, newsType, 1, description.getOrElse(""), NewsRouter.buildRoute(collectionId, collectibleId, newsType), Timestamp.from(Instant.now()))
  }

}
