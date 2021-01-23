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

package modules.user.util

import modules.auth.model.Ticket
import modules.user.model.ViewerCombinator

/**
 * Static functionality to check and compare ViewerCombinator rights.
 * <p> Provides assert methods for the use in service classes.
 */
object ViewerAssertion {

  /**
   * Check if the rights provided by a ViewerCombinator are sufficient to maintain the entity represented by an
   * other ViewerCombinator.
   *
   * @param groupViewerCombinator all possible accesses
   * @param accessViewerCombinator available accesses
   * @return Boolean - true if maintainer rights are available
   */
  def canMaintain(groupViewerCombinator: ViewerCombinator, accessViewerCombinator: ViewerCombinator): Boolean = {
    groupViewerCombinator.maintainers.exists(accessViewerCombinator.maintainers.contains)
  }

  /**
   * Check if the rights provided by a ViewerCombinator are sufficient to edit the entity represented by an
   * other ViewerCombinator.
   *
   * @param groupViewerCombinator all possible accesses
   * @param accessViewerCombinator available accesses
   * @return Boolean - true if editor rights are available
   */
  def canEdit(groupViewerCombinator: ViewerCombinator, accessViewerCombinator: ViewerCombinator): Boolean = {
    canMaintain(groupViewerCombinator, accessViewerCombinator) ||
      groupViewerCombinator.editors.exists(accessViewerCombinator.editors.contains)
  }

  /**
   * Check if the rights provided by a ViewerCombinator are sufficient to view the entity represented by an
   * other ViewerCombinator.
   *
   * @param groupViewerCombinator all possible accesses
   * @param accessViewerCombinator available accesses
   * @return Boolean - true if viewer rights are available
   */
  def canView(groupViewerCombinator: ViewerCombinator, accessViewerCombinator: ViewerCombinator): Boolean = {
    canEdit(groupViewerCombinator, accessViewerCombinator) ||
      groupViewerCombinator.viewers.exists(accessViewerCombinator.viewers.contains)
  }

  /**
   * Check if the rights provided by a Ticket are sufficient to maintain the entity represented by an
   * ViewerCombinator.
   * <p> Throws an exception if rights re not sufficient.
   *
   * @param groupViewerCombinator all possible accesses of the entity
   * @param ticket implicit authentication ticket with rights of the user
   */
  def assertMaintain(groupViewerCombinator: ViewerCombinator)(implicit ticket: Ticket): Unit = {
    if(!canMaintain(groupViewerCombinator, ticket.accessRights)) throw new Exception("No maintainer rights")
  }

  /**
   * Check if the rights provided by a Ticket are sufficient to edit the entity represented by an
   * ViewerCombinator.
   * <p> Throws an exception if rights re not sufficient.
   *
   * @param groupViewerCombinator all possible accesses of the entity
   * @param ticket implicit authentication ticket with rights of the user
   */
  def assertEdit(groupViewerCombinator: ViewerCombinator)(implicit ticket: Ticket): Unit = {
    if(!canEdit(groupViewerCombinator, ticket.accessRights)) throw new Exception("No editor rights")
  }

  /**
   * Check if the rights provided by a Ticket are sufficient to view the entity represented by an
   * ViewerCombinator.
   * <p> Throws an exception if rights re not sufficient.
   *
   * @param groupViewerCombinator all possible accesses of the entity
   * @param ticket implicit authentication ticket with rights of the user
   */
  def assertView(groupViewerCombinator: ViewerCombinator)(implicit ticket: Ticket): Unit = {
    if(!canView(groupViewerCombinator, ticket.accessRights)) throw new Exception("No viewer rights")
  }

}
