-- !Ups

create table `asset_type` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `value` VARCHAR(255) NOT NULL UNIQUE,
    `active` BOOL NOT NULL
);

create table `asset_constraint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `c` VARCHAR(255) NOT NULL,
    `v1` VARCHAR(255) NOT NULL,
    `v2` VARCHAR(255) NOT NULL,
    `type_id` BIGINT NOT NULL,
    FOREIGN KEY(type_id) REFERENCES asset_type(id)
);

create table `asset` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `type_id` BIGINT NOT NULL,
    FOREIGN KEY(type_id) REFERENCES asset_type(id)
);

create table `asset_property` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `key` VARCHAR(255) NOT NULL,
    `value` VARCHAR(255) NOT NULL,
    `parent_id` BIGINT NOT NULL,
    FOREIGN KEY(parent_id) REFERENCES asset(id)
);

create table `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(255) NOT NULL UNIQUE,
    `email` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(255) NOT NULL,
    `key` VARCHAR(255),
    `accepted` BOOL NOT NULL,
    `enabled` BOOL NOT NULL
);

create table `u_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL UNIQUE
);

create table `asset_viewer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `asset_id` BIGINT NOT NULL,
    `group_id` BIGINT NOT NULL,
    FOREIGN KEY(asset_id) REFERENCES asset(id),
    FOREIGN KEY(group_id) REFERENCES u_group(id)
);

create table `group_membership` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    FOREIGN KEY(group_id) REFERENCES u_group(id),
    FOREIGN KEY(user_id) REFERENCES user(id)
);

INSERT INTO u_group(id, name) VALUES(0, 'public');

-- !Downs

drop table `group_membership`;
drop table `asset_viewer`;
drop table `u_group`;
drop table `user`;
drop table `asset_property`;
drop table `asset_constraint`;
drop table `asset`;
drop table `asset_type`;