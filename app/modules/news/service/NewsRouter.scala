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

import modules.news.model.NewsType

/**
 * Object providing static functionality to build endpoint get routes from id specifications.
 * <p> Those routes are used by the news module to provide links without explicitly joining other entities.
 */
object NewsRouter {

  /**
   * Build a route targeting a [[modules.subject.model.Collection Collection]].
   * <p> If the [[modules.news.model.NewsType NewsType]] is DELETED, the link will be empty because no link target
   * will exist.
   *
   * @param collectionId id of the Collection
   * @param newsType     NewsType o which triggered the [[modules.news.model.NewsEvent NewsEvent]]
   * @return String
   */
  def buildRoute(collectionId: Long, newsType: NewsType.Value): String = {
    if (newsType != NewsType.DELETED) {
      "/collection/" + collectionId.toString
    } else {
      "#"
    }
  }

  /**
   * Build a route targeting a [[modules.subject.model.Collectible Collectible]].
   * * <p> If the [[modules.news.model.NewsType NewsType]] is DELETED, the link will be empty because no link target
   * * will exist.
   *
   * @param collectionId  id of the parent [[modules.subject.model.Collection Collection]]
   * @param collectibleId id of the Collectible
   * @param newsType      newsType NewsType o which triggered the [[modules.news.model.NewsEvent NewsEvent]]
   * @return String
   */
  def buildRoute(collectibleId: Long, collectionId: Long, newsType: NewsType.Value): String = {
    if (newsType != NewsType.DELETED) {
      "/collection/" + collectionId.toString
    } else {
      "#"
    }
  }

}
