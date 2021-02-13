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
 * Collectible is a subtype of [[modules.core.model.FlimeyEntity FlimeyEntity]]. It represents a single process step and
 * can not contain other entities
 * In other words, a Collectible is a leaf of the [[modules.subject.model.Collection Collection]] tree.
 * <p> Has a repository representation.
 *
 * @param id           unique identifier
 * @param entityId     id of the parent FlimeyEntity
 * @param collectionId id of the Collection which contains the Collectible
 * @param typeVersionId       id of the parent TypeVersion
 * @param state        [[modules.subject.model.SubjectState SubjectState]] of the Collectible
 * @param created      creation timestamp
 */
case class Collectible(id: Long, entityId: Long, collectionId: Long, typeVersionId: Long, state: SubjectState.State, created: Timestamp)

object Collectible {

  def applyRaw(id: Long, entityId: Long, collectionId: Long, typeVersionId: Long, state: String, created: Timestamp): Collectible = {
    Collectible(id, entityId, collectionId, typeVersionId, SubjectState.withName(state), created)
  }

  def unapplyToRaw(arg: Collectible): Option[(Long, Long, Long, Long, String, Timestamp)] =
    Option((arg.id, arg.entityId, arg.collectionId, arg.typeVersionId, arg.state.toString, arg.created))

  val tupledRaw: ((Long, Long, Long, Long, String, Timestamp)) => Collectible = (this.applyRaw _).tupled

}