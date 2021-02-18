/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020-2021 Karl Kegel
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

create table flimey_entity
(
    id SERIAL NOT NULL PRIMARY KEY
);

create table entity_type
(
    id      SERIAL       NOT NULL PRIMARY KEY,
    value   VARCHAR(255) NOT NULL UNIQUE,
    type_of VARCHAR(255) NOT NULL,
    active  BOOL         NOT NULL
);

create table type_version
(
    id      SERIAL NOT NULL PRIMARY KEY,
    type_id BIGINT NOT NULL,
    version BIGINT NOT NULL,
    FOREIGN KEY (type_id) REFERENCES entity_type (id)
);

create table type_constraint
(
    id              SERIAL       NOT NULL PRIMARY KEY,
    c               VARCHAR(255) NOT NULL,
    v1              VARCHAR(255) NOT NULL,
    v2              VARCHAR(255) NOT NULL,
    by_plugin       VARCHAR(255),
    type_version_id BIGINT       NOT NULL,
    FOREIGN KEY (type_version_id) REFERENCES type_version (id)
);

create table collection
(
    id              SERIAL       NOT NULL PRIMARY KEY,
    type_version_id BIGINT       NOT NULL,
    entity_id       BIGINT       NOT NULL,
    status          VARCHAR(255) NOT NULL,
    created         TIMESTAMP    NOT NULL,
    FOREIGN KEY (entity_id) REFERENCES flimey_entity (id),
    FOREIGN KEY (type_version_id) REFERENCES type_version (id)
);

create table collectible
(
    id              SERIAL       NOT NULL PRIMARY KEY,
    entity_id       BIGINT       NOT NULL,
    collection_id   BIGINT       NOT NULL,
    type_version_id BIGINT       NOT NULL,
    state           VARCHAR(255) NOT NULL,
    created         TIMESTAMP    NOT NULL,
    FOREIGN KEY (entity_id) REFERENCES flimey_entity (id),
    FOREIGN KEY (type_version_id) REFERENCES type_version (id),
    FOREIGN KEY (collection_id) REFERENCES collection (id)
);


create table asset
(
    id              SERIAL NOT NULL PRIMARY KEY,
    entity_id       BIGINT NOT NULL,
    type_version_id BIGINT NOT NULL,
    FOREIGN KEY (entity_id) REFERENCES flimey_entity (id),
    FOREIGN KEY (type_version_id) REFERENCES type_version (id)
);

create table property
(
    id        SERIAL       NOT NULL PRIMARY KEY,
    pkey      VARCHAR(255) NOT NULL,
    value     VARCHAR(255) NOT NULL,
    parent_id BIGINT       NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES flimey_entity (id)
);

create table f_user
(
    id       SERIAL       NOT NULL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email    VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role     VARCHAR(255) NOT NULL,
    auth_key VARCHAR(255),
    accepted BOOL         NOT NULL,
    enabled  BOOL         NOT NULL
);

create table u_group
(
    id   SERIAL       NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

create table entity_viewer
(
    id        SERIAL       NOT NULL PRIMARY KEY,
    target_id BIGINT       NOT NULL,
    viewer_id BIGINT       NOT NULL,
    role      VARCHAR(255) NOT NULL,
    FOREIGN KEY (target_id) REFERENCES flimey_entity (id),
    FOREIGN KEY (viewer_id) REFERENCES u_group (id),
    CONSTRAINT c_entity_target_viewer UNIQUE (target_id, viewer_id)
);

create table group_viewer
(
    id        SERIAL       NOT NULL PRIMARY KEY,
    target_id BIGINT       NOT NULL,
    viewer_id BIGINT       NOT NULL,
    role      VARCHAR(255) NOT NULL,
    FOREIGN KEY (target_id) REFERENCES u_group (id),
    FOREIGN KEY (viewer_id) REFERENCES u_group (id),
    CONSTRAINT c_group_target_viewer UNIQUE (target_id, viewer_id)
);

create table group_membership
(
    id       SERIAL NOT NULL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id  BIGINT NOT NULL,
    FOREIGN KEY (group_id) REFERENCES u_group (id),
    FOREIGN KEY (user_id) REFERENCES f_user (id),
    CONSTRAINT c_group_user UNIQUE (group_id, user_id)
);

create table news_event
(
    id          SERIAL       NOT NULL PRIMARY KEY,
    news_type   VARCHAR(255) NOT NULL,
    priority    BIGINT       NOT NULL,
    description VARCHAR(255) NOT NULL,
    route       VARCHAR(255) NOT NULL,
    date        TIMESTAMP    NOT NULL
);

create table news_target
(
    id       SERIAL NOT NULL PRIMARY KEY,
    news_id  BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    FOREIGN KEY (news_id) REFERENCES news_event (id),
    FOREIGN KEY (group_id) REFERENCES u_group (id)
);

-- Default inserts on installation
INSERT INTO u_group(id, name)
VALUES (1, 'public');
INSERT INTO u_group(id, name)
VALUES (2, 'system');
INSERT INTO f_user(id, username, email, password, role, auth_key, accepted, enabled)
VALUES (1, 'System', NULL, NULL, 'SYSTEM', 'root', false, false);
INSERT INTO group_membership(id, group_id, user_id)
VALUES (1, 2, 1);

SELECT pg_catalog.setval(pg_get_serial_sequence('u_group', 'id'), (SELECT MAX(id) FROM u_group) + 1);
SELECT pg_catalog.setval(pg_get_serial_sequence('f_user', 'id'), (SELECT MAX(id) FROM f_user) + 1);
SELECT pg_catalog.setval(pg_get_serial_sequence('group_membership', 'id'), (SELECT MAX(id) FROM group_membership) + 1);

-- !Downs

drop table news_target;
drop table news_event;
drop table group_membership;
drop table entity_viewer;
drop table group_viewer;
drop table u_group;
drop table f_user;
drop table property;
drop table type_constraint;
drop table asset;
drop table collectible;
drop table collection;
drop table type_version;
drop table entity_type;
drop table flimey_entity;