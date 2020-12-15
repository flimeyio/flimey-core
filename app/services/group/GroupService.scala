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

package services.group

import com.google.inject.Inject
import db.group.GroupRepository
import db.user.UserRepository
import model.auth.Ticket
import model.group.Group
import model.user.RoleAssertion

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
  //def getFirstClassGroupTransitions(groupId: Long)(implicit ticket: Ticket): Future[GroupViewerRelation] = {
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

  //TODO
  // def getAllGroupsOfUser()

  //TODO
  // def addGroup()

  //TODO
  // def deleteGroup()

  //TODO
  // def renameGroup()

  //TODO
  // def addRelation()

  //TODO
  // def removeRelation()

  //TODO
  // def updateRelationRole()
}
