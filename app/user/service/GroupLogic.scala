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

package user.service

import user.model.{Group, GroupViewerCombinator, Viewer, ViewerRole}

object GroupLogic extends ViewerProcessor {

  def buildGroupViewerCombinator(relations: Seq[(Group, Viewer)]): GroupViewerCombinator = {
    var maintainers: Set[Group] = Set()
    var editors: Set[Group] = Set()
    var viewers: Set[Group] = Set()
    relations.foreach(rel => {
      val (group, viewer) = rel
      if(ViewerRole.isAtLeastMaintainer(viewer.role)){
        maintainers = maintainers + group
      } else if(ViewerRole.isAtLeastEditor(viewer.role)){
        editors = editors + group
      } else if(ViewerRole.isAtLeastViewer(viewer.role)){
        viewers = viewers + group
      }
    })
    GroupViewerCombinator(viewers, editors, maintainers)
  }

}
