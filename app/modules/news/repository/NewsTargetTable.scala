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

import modules.news.model.NewsTarget
import slick.jdbc.PostgresProfile.api._

class NewsTargetTable(tag: Tag) extends Table[NewsTarget](tag, "news_target") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def newsId = column[Long]("news_id")

  def groupId = column[Long]("group_id")

  override def * = (id, newsId, groupId) <> (NewsTarget.tupled, NewsTarget.unapply)

}