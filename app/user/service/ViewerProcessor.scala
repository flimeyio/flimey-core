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

import user.model.{Group, Viewer, ViewerCombinator, ViewerRole}

/**
 * Trait which provides helper functionality for working with Viewers.
 */
trait ViewerProcessor {

  /**
   * Derive Viewer objects from a given raw string collection.
   * <p> Duplicates and invalid names will be filtered and ignored.
   * <p> If names occur in different role categories, only the highest role will become a Viewer.
   * <p> All ids and viewerIds in the result Viewers are set to 0.
   *
   * @param maintainers names of Groups that will receive maintainer rights
   * @param editors     names of Groups that will receive editor rights
   * @param viewers     names of Groups that will receive viewer rights
   * @param allGroups   all possible Groups to match the given names
   * @return Seq[Viewer]
   */
  def deriveViewersFromData(maintainers: Seq[String], editors: Seq[String], viewers: Seq[String],
                            allGroups: Seq[Group]): Seq[Viewer] = {
    //create viewers with MAINTAINER rights
    val maintainViewers = matchViewGroups(maintainers, allGroups, ViewerRole.MAINTAINER)
    //create viewers with EDITOR rights and remove duplicates already in maintainers
    val editViewers = matchViewGroups(
      editors.filter(v => !maintainers.contains(v)),
      allGroups,
      ViewerRole.EDITOR)
    //create viewers with VIEWER rights and remove duplicates already in maintainers or editors
    val viewViewers = matchViewGroups(
      viewers.filter(v => !maintainers.contains(v) && !editors.contains(v)),
      allGroups,
      ViewerRole.VIEWER)
    //return sum of viewers
    maintainViewers ++ editViewers ++ viewViewers
  }

  /**
   * Build Viewers of a specified ViewerRole from given Group names.
   * <p> Invalid names and duplicates are filtered out.
   * <p> The id and viewerId of the result Viewers are set to 0.
   *
   * @param names  of the Groups that should become Viewers
   * @param groups all possible Groups to match the given names
   * @param role   ViewerRole the Viewers will receive
   * @return Seq[Viewer]
   */
  private def matchViewGroups(names: Seq[String], groups: Seq[Group], role: ViewerRole.Role): Seq[Viewer] = {
    val groupNames = groups.map(_.name)
    val viewGroupNames = names.filter(groupNames.contains(_)).toSet
    viewGroupNames.map(name => Viewer(0, 0, groups.find(_.name == name).get.id, role)).toSeq
  }

  /**
   * <p> (1) Filter out the Viewers, that are not represented in a sequence of Group names.
   * <p> (2) Build the set of Viewers, that are not already part of a given ViewerCombinator of a specified target.
   * <p> Duplicates and invalid Group names are handled and filtered out.
   * <p> See [[ViewerProcessor.deriveViewersFromData]]
   * <p> NOTE: THIS METHOD DOES NOT CHECK ANY SYSTEM GROUP CONSTRAINTS!
   *
   * @param maintainers names of Groups which are or should become maintainers
   * @param editors names of Groups which are or should become editors
   * @param viewers names of Groups which are or should become viewers
   * @param oldViewers ViewerCombinator of all already existing viewers
   * @param allGroups Groups to match the viewer candidates
   * @param targetId id of the viewed entity
   * @return (viewersToDelete, viewersToAdd) where viewersToDelete is a set of Group ids which viewers must be deleted.
   *         viewersToAdd is a set of complete Viewer objects which must be added to the Asset.
   */
  def getViewerChanges(maintainers: Set[String], editors: Set[String], viewers: Set[String],
                       oldViewers: ViewerCombinator, allGroups: Seq[Group], targetId: Long): (Set[Long], Set[Viewer]) = {

    val maintainersToDeleteIds = oldViewers.maintainers.filter(v => !maintainers.contains(v.name)).map(_.id)
    val editorsToDeleteIds = oldViewers.editors.filter(v => !editors.contains(v.name)).map(_.id)
    val viewersToDeleteIds = oldViewers.viewers.filter(v => !viewers.contains(v.name)).map(_.id)
    val viewersToDelete = maintainersToDeleteIds ++ editorsToDeleteIds ++ viewersToDeleteIds

    val maintainerNamesToKeep = oldViewers.maintainers.filter(v => !maintainersToDeleteIds.contains(v.id)).map(_.name)
    val editorNamesToKeep = oldViewers.editors.filter(v => !editorsToDeleteIds.contains(v.id)).map(_.name)
    val viewerNamesToKeep = oldViewers.viewers.filter(v => !viewersToDeleteIds.contains(v.id)).map(_.name)

    val newMaintainers = maintainers.diff(maintainerNamesToKeep)
    val newEditors = editors.diff(editorNamesToKeep)
    val newViewers = viewers.diff(viewerNamesToKeep)
    val viewerObjects = deriveViewersFromData(newMaintainers.toSeq, newEditors.toSeq, newViewers.toSeq, allGroups)
    val viewersToAdd = viewerObjects.map(v => Viewer(v.id, targetId, v.viewerId, v.role)).toSet

    (viewersToDelete, viewersToAdd)
  }

  /**
   * Combines many ViewerCombinators into a single one.
   * <p> For each Group, only the position of the highest role is maintained.
   * Duplicates are removed.
   *
   * @param viewerCombinators ViewerCombinators to combine
   * @return ViewerCombinator with all Viewer rights of the single combinators.
   */
  def unifyViewerCombinators(viewerCombinators: Seq[ViewerCombinator]): ViewerCombinator = {
    var maintainers: Set[Group] = Set()
    var editors: Set[Group] = Set()
    var viewers: Set[Group] = Set()
    viewerCombinators.foreach(viewerCombinator => {
      maintainers = maintainers ++ viewerCombinator.maintainers
      editors = editors ++ viewerCombinator.editors
      viewers = viewers ++ viewerCombinator.viewers
    })
    viewers = viewers.diff(editors).diff(maintainers)
    editors = editors.diff(maintainers)
    ViewerCombinator(viewers, editors, maintainers)
  }

}
