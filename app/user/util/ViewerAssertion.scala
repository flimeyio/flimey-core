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

package user.util

import auth.model.Ticket
import user.model.ViewerCombinator

object ViewerAssertion {

  private def canMaintain(groupViewerCombinator: ViewerCombinator)(implicit ticket: Ticket): Boolean = {
    groupViewerCombinator.maintainers.exists(ticket.groups.contains)
  }

  private def canEdit(groupViewerCombinator: ViewerCombinator)(implicit ticket: Ticket): Boolean = {
    canMaintain(groupViewerCombinator) || groupViewerCombinator.editors.exists(ticket.groups.contains)
  }

  private def canView(groupViewerCombinator: ViewerCombinator)(implicit ticket: Ticket): Boolean = {
    canEdit(groupViewerCombinator) || groupViewerCombinator.viewers.exists(ticket.groups.contains)
  }

  def assertMaintain(groupViewerCombinator: ViewerCombinator)(implicit ticket: Ticket): Unit = {
    if(!canMaintain(groupViewerCombinator)) throw new Exception("No maintainer rights")
  }

  def assertEdit(groupViewerCombinator: ViewerCombinator)(implicit ticket: Ticket): Unit = {
    if(!canEdit(groupViewerCombinator)) throw new Exception("No editor rights")
  }

  def assertView(groupViewerCombinator: ViewerCombinator)(implicit ticket: Ticket): Unit = {
    if(!canView(groupViewerCombinator)) throw new Exception("No viewer rights")
  }

}
