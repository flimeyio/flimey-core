# Groups

## Outline

In flimey, groups are the fundamental structure to manage users, their access and the visibility of objects.

## Definition

* a group contains a number of users (empty groups are possible)
* there is at least one group - the *public* group
* every user is always member of the *public* group
* a user can have any number of group memberships greater zero
* only admin users can create, delete and modify groups and their memberships

## Usage with Assets

Every asset has a defined visibility which is set by its associated groups. If an asset is created, the user
can specify, which groups are able to see it and which group is able to edit it. The user can also set the *view* rights for groups he is not member himself.

* members of groups that are granted *view* rights, can the find, use and connect the asset to their respective work
* members of the *edit* group, have *view* rights as well as the option to edit, delete and change the visibility of the asset
* if a group is deleted, all assets of which the group has *edit* rights will per option also be deleted or migrated to the *public* group

## For Developers

ID|Name|Description|Status
---|---|---|---
