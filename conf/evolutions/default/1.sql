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

-- !Downs

drop table `asset_property`;
drop table `asset_constraint`;
drop table `asset`;
drop table `asset_type`;
