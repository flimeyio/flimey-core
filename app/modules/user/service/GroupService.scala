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

package modules.user.service

import modules.auth.model.Ticket
import modules.auth.util.RoleAssertion
import com.google.inject.Inject
import modules.core.model.{Viewer, ViewerRole}
import modules.user.model._
import modules.user.repository.{GroupMembershipRepository, GroupRepository, GroupViewerRepository, UserRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Service class to provide SAFE business logic for Groups, Viewers and their User relations.
 * This class is normally used by dependency injection inside controller endpoints.
 *
 * @param groupRepository           injected db interface for Group entities.
 * @param groupMembershipRepository injected db interface for GroupMembership entities
 * @param userRepository            injected db interface for User entities
 * @param groupViewerRepository     injected db interface for (Group)Viewer entities
 */
class GroupService @Inject()(groupRepository: GroupRepository,
                             groupMembershipRepository: GroupMembershipRepository,
                             userRepository: UserRepository,
                             groupViewerRepository: GroupViewerRepository) {

  /**
   * Get all existing Groups<br />
   * This is a safe implementation and can be used by controller classes.
   * <br />
   * Fails without WORKER rights.
   *
   * @param ticket implicit authentication ticket
   * @return all Groups of the system
   */
  def getAllGroups(implicit ticket: Ticket): Future[Seq[Group]] = {
    try {
      RoleAssertion.assertWorker
      groupRepository.getAll
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get a single Group by its id. fails if no Group can be found.<br />
   * * <p> This is a safe implementation and can be used by controller classes.
   * * <p> Fails without WORKER rights.
   *
   * @param groupId id of the Group
   * @param ticket  implicit authentication ticket
   * @return the Group
   */
  def getGroup(groupId: Long)(implicit ticket: Ticket): Future[Group] = {
    try {
      RoleAssertion.assertWorker
      groupRepository.getById(groupId) map (groupOption => {
        if (groupOption.isEmpty) throw new Exception("No such Group found")
        groupOption.get
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all Groups of a single User.<br />
   * The User is specified by his authentication ticket.<br />
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without WORKER rights.
   *
   * @param ticket implicit authentication ticket
   * @return groups of the calling User
   */
  def getGroupsOfUser(implicit ticket: Ticket): Future[Seq[Group]] = {
    try {
      RoleAssertion.assertWorker
      groupRepository.getAllOfUser(ticket.authSession.userId)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all Users (members) of a single Group.<br />
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without WORKER rights.
   *
   * @param groupId if of the Group
   * @param ticket  implicit authentication ticket
   * @return Users of the Group
   */
  def getUsersOfGroup(groupId: Long)(implicit ticket: Ticket): Future[Seq[User]] = {
    try {
      RoleAssertion.assertWorker
      groupMembershipRepository.getMembersOfGroup(groupId)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Add a new Group.<br />
   * The provided name must be unique. The name is further handled in lower case.<br />
   * <p> This operation requires at least admin rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param name   unique name of the new Group
   * @param ticket implicit authentication ticket
   * @return group id on success
   */
  def addGroup(name: String)(implicit ticket: Ticket): Future[Long] = {
    try {
      RoleAssertion.assertAdmin
      //FIXME check name for length and empty (if unique is checked by the db)
      groupRepository.add(Group(0, name.toLowerCase))
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  //TODO
  // def renameGroup()

  /**
   * Delete a Group by its id.<br />
   * This operation also deletes all Viewer relations and memberships regarding this Group.
   * The "system" and "public" groups can not be deleted
   * <br />
   * <p> This operation requires at least admin rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param groupId id of the group to delete (but not 1 or 2)
   * @param ticket  implicit authentication ticket
   * @return unit on success
   */
  def deleteGroup(groupId: Long)(implicit ticket: Ticket): Future[Unit] = {
    try {
      RoleAssertion.assertAdmin
      groupRepository.getById(groupId) flatMap (groupOption => {
        if (groupOption.isEmpty) throw new Exception("Invalid Group")
        val group = groupOption.get
        if (group.name == GroupStats.PUBLIC_GROUP || group.name == GroupStats.SYSTEM_GROUP){
          throw new Exception("The public and system groups can not be deleted.")
        }
        groupRepository.delete(groupId)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Add a User as member to a Group.<br />
   * <p> This operation requires at least admin rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param groupId id of the Group to add the User
   * @param email   of the User to add
   * @param ticket  implicit authentication ticket
   * @return id of the new membership
   */
  def addMembership(groupId: Long, email: String)(implicit ticket: Ticket): Future[Long] = {
    try {
      RoleAssertion.assertAdmin
      userRepository.getByEMail(email) flatMap (userOption => {
        if (userOption.isEmpty) throw new Exception("No such User found")
        val user = userOption.get
        groupMembershipRepository.add(GroupMembership(0, groupId, user.id))
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Delete a User from a Group<br />
   * No User can be removed from the "public" Group.
   * SYSTEM Users can not be removed from the "system" Group.<br />
   * <p> This operation requires at least admin rights
   * <p> This is a safe implementation and can be used by controller classes.
   *
   * @param groupId id of the Group to remove the User from
   * @param userId  id of the User to remove from the Group
   * @param ticket  implicit authentication ticket
   * @return status future
   */
  def deleteMembership(groupId: Long, userId: Long)(implicit ticket: Ticket): Future[Int] = {
    try {
      RoleAssertion.assertAdmin
      groupRepository.getById(groupId) flatMap (groupOption => {
        if (groupOption.isEmpty) throw new Exception("Invalid Group")
        val group = groupOption.get
        if (group.name == "public") throw new Exception("No user can be removed from this group")
        userRepository.getById(userId) flatMap (userOption => {
          if (userOption.isEmpty) throw new Exception("Invalid User")
          val user = userOption.get
          if (Role.isAtLeastSystem(user.role) && group.name == "system") {
            throw new Exception("The SYSTEM user can not be removed from this group")
          }
          groupMembershipRepository.delete(userId, groupId)
        })
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all Groups with their rights (ViewerCombinator) which
   * are DIRECT (first class) viewers of the target group.<br />
   * This is a safe implementation and can be used by controller classes.
   * <br />
   * Fails without WORKER rights.
   *
   * @param groupId id of the target (viewed) group
   * @param ticket  implicit authentication ticket
   * @return Future[ViewerCombinator]
   */
  def getFirstClassGroupViewers(groupId: Long)(implicit ticket: Ticket): Future[ViewerCombinator] = {
    try {
      RoleAssertion.assertWorker
      groupViewerRepository.getFirstClassViewers(groupId) map (relations => {
        ViewerCombinator.fromRelations(relations)
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Get all Groups (ViewerCombinator) that are viewing target of a given Group.
   * <p> This operation needs no rights to execute, but should only used for session creation during login.
   *
   * @param groupId id of the viewing group
   * @return Future[ViewerCombinator]
   */
  def getFirstClassTargets(groupId: Long): Future[ViewerCombinator] = {
    groupViewerRepository.getFirstClassTargets(groupId) map (relations => {
      ViewerCombinator.fromRelations(relations)
    })
  }

  /**
   * Add a new GroupViewerRelation.<br />
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without ADMIN rights.
   *
   * @param targetId   id of the Group to be viewed
   * @param viewerName name of the Group which views the target
   * @param viewerRole ViewerRole string of the role the viewer will have
   * @param ticket     implicit authentication ticket
   * @return Future[Long]
   */
  def addRelation(targetId: Long, viewerName: String, viewerRole: String)(implicit ticket: Ticket): Future[Long] = {
    try {
      RoleAssertion.assertAdmin
      val roleStatus = ViewerRole.parseViewerRole(viewerRole)
      groupRepository.getByName(viewerName) flatMap (viewerOption => {
        if (viewerOption.isEmpty) throw new Exception("No such viewer group found")
        val viewerGroup = viewerOption.get
        if(viewerGroup.id == targetId) throw new Exception("No self-relations possible")
        groupViewerRepository.add(Viewer(0, targetId, viewerGroup.id, roleStatus))
      })
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  /**
   * Remove an existing GroupViewerRelation.<br />
   * <p> This is a safe implementation and can be used by controller classes.
   * <p> Fails without ADMIN rights.
   *
   * @param targetId id of the viewed Group
   * @param viewerId if of the viewing Group
   * @param ticket   implicit authentication ticket
   * @return Future[Int]
   */
  def removeRelation(targetId: Long, viewerId: Long)(implicit ticket: Ticket): Future[Int] = {
    try {
      RoleAssertion.assertAdmin
      groupViewerRepository.delete(targetId, viewerId)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

}
