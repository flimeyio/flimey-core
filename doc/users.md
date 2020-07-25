#Users

##Outline

A *user* in the flimey system means always an account identified by a (visible) name and a 
corresponding (unique) email address. Only admins can create, modify and delete users. Therefore, an initial
admin account is always present in the associated database.

##Roles

Every user has a role given by an admin. Those roles are USER (default), MODELER and ADMIN.

* USER can access read and edit assets, collections and other business subjects.
* MODELER is a USER and has additional access to the *model* section, where asset types and subject types can be edited.
* ADMIN is a MODELER and has additional access to the *admin* section, where accounts, roles and groups are managed.

Roles should always be grant in a restrictive way. Otherwise, the role system will lose its advantages.
It is also recommended dividing concerns by giving some people different accounts. For example the system admin should use 
his ADMIN account only for administration. However, he can have a second USER account for daily work.

##Usage

An active account can be used to log in the flimey system. Note: an account works always only for the flimey-core instance 
where it was created. For different servers, a new account must be created.

* to log in, go to *Account* -> *Log In*
* to log out, go to *Account* -> *Log Out* (only visible after log in)
* to activate a newly created account go to *Account* -> *Authenticate*

##Account Creation

Only admins can create user accounts. To create a new account:

* go to *Admin* -> *Create User*
* enter the user data (email is checked by the system to be unique)
* the system returns an authentication key. Copy that key and send (email) it to the user. If SMTP is configured, the key 
will be sent automatically to the given mail address.

##Account Change

Only admins can update user data:

* go to *Admin* -> *Update User*
* enter the email of the user you want to edit.
* change and save the displayed data.

##Account Deletion

Only admins can create user accounts. The initial admin account cannot be deleted.

* go to *Admin* -> *Delete User*
* enter the email of the user you want to delete.
* delete the user.

During deletion, all associated items of the user may be rearranged to stay consistent. 

##For Developers

//TODO