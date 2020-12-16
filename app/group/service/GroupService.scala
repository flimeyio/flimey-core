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

package group.service

import auth.model.Ticket
import com.google.inject.Inject
import group.model.Group
import group.repository.GroupRepository
import user.repository.UserRepository
import util.assertions.RoleAssertion

import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic for Groups, Viewers and their User relations.
 * This class is normally used by dependency injection inside controller endpoints.
 *
 * @param groupRepository injected db interface for Group entities.
 * @param userRepository injected db interface for User entities.
 */
class GroupService @Inject()(groupRepository: GroupRepository, userRepository: UserRepository) extends RoleAssertion {

  /**
   * Get all existing Groups<br />
   * This is a safe implementation and can be used by controller classes.
   * <br />
   * Fails without WORKER rights.
   *
   * @param ticket implicit authentication ticket
   * @return
   */
  def getAllGroups(implicit ticket: Ticket): Future[Seq[Group]] = {
    try {
      assertWorker
      groupRepository.getAll
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  ///**
  // * Get all Groups with their Rights (GroupViewerRelation) which are direct viewer of the target group.<br />
  // * This is a safe implementation and can be used by controller classes.
  // * <br />
  // * Fails without WORKER rights.
  // *
  // * @param groupId id of the target (viewed) group
  // * @param ticket implicit authentication ticket
  // * @return
  // */
  //def getFirstClassGroupTransition(groupId: Long)(implicit ticket: Ticket): Future[GroupViewerRelation] = {
  //  //TODO
  //}

  ///**
  // * Get all Groups with their Rights (GroupViewerRelation) which are direct or indirect viewer of the target group.<br />
  // * The search resolves viewer cycles, so that the result contains each viewer only once.
  // * <p>
  // * Per definition of the viewer relation, a far away transitive viewer receives the minimum rights of its predecessors.
  // * However, if the minimum of the predecessors is higher that its own, it will keep its own lower right.
  // * <p>
  // * This is a safe implementation and can be used by controller classes.
  // * <br />
  // *
  // * @param groupId id of the target (viewed) group
  // * @param ticket implicit authentication ticket
  // * @return
  // */
  //def getTransitiveGroupTransitionClosure(groupId: Long)(implicit ticket: Ticket): Future[GroupViewerRelation] = {
  //  //TODO
  //}

  /**
   * Add a new Group.<br />
   * The provided name must be unique.<br />
   * <p> This operation requires at least admin rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param name unique name of the new Group
   * @param ticket implicit authentication ticket
   * @return group id on success
   */
  def addGroup(name: String)(implicit ticket: Ticket): Future[Long] = {
    try {
      assertAdmin
      //FIXME check name for length and empty (if unique is checked by the db)
      groupRepository.add(Group(0, name))
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete a Group by its id.<br />
   * This operation also deletes all Viewer relations and memberships regarding this Group.<br />
   * <p> This operation requires at least admin rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param groupId id of the group to delete (but not 1 or 2)
   * @param ticket implicit authentication ticket
   * @return unit on success
   */
  def deleteGroup(groupId: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      assertAdmin
      if(groupId == 1 || groupId == 2) throw new Exception("The public and system groups can not be deleted.")
      groupRepository.delete(groupId)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  //TODO
  // def renameGroup()

  //TODO
  // def addRelation()

  //TODO
  // def removeRelation()

  //TODO
  // def updateRelationRole()
}
