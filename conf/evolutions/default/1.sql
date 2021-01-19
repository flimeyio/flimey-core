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

-- !Ups

create table `flimey_entity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY
);

create table `entity_type` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `value` VARCHAR(255) NOT NULL UNIQUE,
    `type_of` VARCHAR(255) NOT NULL,
    `active` BOOL NOT NULL
);

create table `constraint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `c` VARCHAR(255) NOT NULL,
    `v1` VARCHAR(255) NOT NULL,
    `v2` VARCHAR(255) NOT NULL,
    `by_plugin` VARCHAR(255),
    `type_id` BIGINT NOT NULL,
    FOREIGN KEY(type_id) REFERENCES entity_type(id)
);

create table `collection` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `type_id` BIGINT NOT NULL,
    `entity_id` BIGINT NOT NULL,
    `name` VARCHAR(255) NOT NULL UNIQUE,
    `status` VARCHAR(255) NOT NULL,
    `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(entity_id) REFERENCES flimey_entity(id),
    FOREIGN KEY(type_id) REFERENCES entity_type(id)
);

create table `asset` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `entity_id` BIGINT NOT NULL,
    `type_id` BIGINT NOT NULL,
    FOREIGN KEY(entity_id) REFERENCES flimey_entity(id),
    FOREIGN KEY(type_id) REFERENCES entity_type(id)
);

create table `property` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `key` VARCHAR(255) NOT NULL,
    `value` VARCHAR(255) NOT NULL,
    `parent_id` BIGINT NOT NULL,
    FOREIGN KEY(parent_id) REFERENCES flimey_entity(id)
);

create table `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(255) NOT NULL UNIQUE,
    `email` VARCHAR(255) UNIQUE,
    `password` VARCHAR(255),
    `role` VARCHAR(255) NOT NULL,
    `auth_key` VARCHAR(255),
    `accepted` BOOL NOT NULL,
    `enabled` BOOL NOT NULL
);

create table `u_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL UNIQUE
);

create table `entity_viewer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `target_id` BIGINT NOT NULL,
    `viewer_id` BIGINT NOT NULL,
    `role` VARCHAR(255) NOT NULL,
    FOREIGN KEY(target_id) REFERENCES flimey_entity(id),
    FOREIGN KEY(viewer_id) REFERENCES u_group(id),
    UNIQUE KEY (`target_id`, `viewer_id`)
);

create table `group_viewer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `target_id` BIGINT NOT NULL,
    `viewer_id` BIGINT NOT NULL,
    `role` VARCHAR(255) NOT NULL,
    FOREIGN KEY(target_id) REFERENCES u_group(id),
    FOREIGN KEY(viewer_id) REFERENCES u_group(id),
    UNIQUE KEY (`target_id`, `viewer_id`)
);

create table `group_membership` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    FOREIGN KEY(group_id) REFERENCES u_group(id),
    FOREIGN KEY(user_id) REFERENCES user(id),
    UNIQUE KEY (`group_id`, `user_id`)
);

-- Default inserts on installation
INSERT INTO u_group(id, name) VALUES(1, 'public');
INSERT INTO u_group(id, name) VALUES(2, 'system');
INSERT INTO user(id, username, email, password, role, auth_key, accepted, enabled) VALUES(1, 'System', NULL, NULL, 'SYSTEM', 'root', false, false);
INSERT INTO group_membership(id, group_id, user_id) VALUES (1, 2, 1);

-- !Downs

drop table `group_membership`;
drop table `entity_viewer`;
drop table `group_viewer`;
drop table `u_group`;
drop table `user`;
drop table `property`;
drop table `constraint`;
drop table `asset`;
drop table `collection`;
drop table `entity_type`;
drop table `flimey_entity`;