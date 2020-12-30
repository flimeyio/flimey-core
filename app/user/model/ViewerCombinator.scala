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

package user.model

/**
 * Model class representing the viewer and editor group relations of a target group.
 * This class can be used to represent the first-class relations (only direct descendents) or the complete transitive closure.
 *
 * @param viewers groups that can only view the content of the target
 * @param editors groups that can view and edit the content of the target
 * @param maintainers groups that can delete, administrate of migrate the target
 */
case class ViewerCombinator(viewers: Set[Group], editors: Set[Group], maintainers: Set[Group]){

  def getAllViewingGroups: Set[Group] = viewers ++ editors ++ maintainers
  def getAllViewingGroupIds: Set[Long] = getAllViewingGroups.map(_.id)

  def getAllEditingGroups: Set[Group] = editors ++ maintainers
  def getAllEditingGroupIds: Set[Long] = getAllEditingGroups.map(_.id)

  def getAllMaintainingGroups: Set[Group] = maintainers
  def getAllMaintainingGroupIds: Set[Long] = getAllMaintainingGroups.map(_.id)

}

object ViewerCombinator {

  def fromRelations(relations: Seq[(Group, Viewer)]): ViewerCombinator = {
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
    ViewerCombinator(viewers, editors, maintainers)
  }

}
