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

package modules.subject.service

import modules.subject.model.{CollectionHeader, SubjectState}
import modules.util.messages.{ERR, OK, Status}

/**
 * Trait which provides functionality for parsing and processing the [[modules.subject.model.SubjectState SubjectState]].
 */
trait SubjectStateProcessor {

  def parseState(value: String): SubjectState.State = {
    try {
      SubjectState.withName(value)
    } catch {
      case e: Throwable => throw new Exception("Invalid state value")
    }
  }

  def isValidStateTransition(oldState: SubjectState.State, newState: SubjectState.State): Status = {
    if (newState == SubjectState.CREATED) return ERR("This state can not be entered again")
    OK()
  }

  def isReadyToArchive(collectionHeader: CollectionHeader): Status = {
    val hasOpenChildren = collectionHeader.collectibles.map(_.collectible.state).exists(
      state => state != SubjectState.CLOSED_SUCCESS && state != SubjectState.CLOSED_FAILURE)
    if (hasOpenChildren) {
      ERR("All sub elements must be closed before the parent element can be archived")
    } else {
      OK()
    }
  }
}
