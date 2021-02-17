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

package modules.news.model

import java.sql.Timestamp

case class NewsEvent(id: Long, newsType: NewsType.Value, priority: Long, description: String, route: String, date: Timestamp)

object NewsEvent {

  def applyRaw (id: Long, newsType: String, priority: Long, description: String, route: String, date: Timestamp): NewsEvent = {
    NewsEvent(id, NewsType.withName(newsType), priority, description, route, date)
  }

  def unapplyToRaw(arg: NewsEvent): Option[(Long, String, Long, String, String, Timestamp)] =
    Option((arg.id, arg.newsType.toString, arg.priority, arg.description, arg.route, arg.date))

  val tupledRaw: ((Long, String, Long, String, String, Timestamp)) => NewsEvent = (this.applyRaw _).tupled

}