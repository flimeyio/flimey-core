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

package modules.subject.model

import java.sql.Timestamp

/**
 * A Subject is a basic time and process based entity.
 * Logical super class for both Collection and Collectible.
 * <p> Has a repository representation.
 *
 * @param id unique identifier
 *           @param entityId id of the parent FlimeyEntity
 * @param name short and descriptive name
 * @param status progress status - see [[SubjectStatus]]
 * @param created time of creation
 */
case class Subject(id: Long, entityId: Long, name: String, status: SubjectStatus.Status, created: Timestamp)

object Subject {

  def applyRaw (id: Long, entityId: Long, name: String, status: String, created: Timestamp): Subject = {
    Subject(id, entityId, name, SubjectStatus.withName(status), created)
  }

  def unapplyToRaw(arg: Subject): Option[(Long, Long, String, String, Timestamp)] =
    Option((arg.id, arg.entityId, arg.name, arg.status.toString, arg.created))

  val tupledRaw: ((Long, Long, String, String, Timestamp)) => Subject = (this.applyRaw _).tupled

}
