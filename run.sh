#!/bin/bash

echo "starting the flimey core service"

echo "check postgres 12 installation"

service postgresql stop

if [ ! -f "/flimeydata/moved.txt" ]; then
	echo "move postgres data directory to mounted volume"
	chown postgres:postgres /flimeydata
	chmod 700 /flimeydata
	apt-get -y install rsync
	rsync -av /var/lib/postgresql/12/main /flimeydata
	touch /flimeydata/moved.txt
fi

if [ ! -f "/var/lib/postgresql/reconfigured.txt" ]; then
	echo "configure postgres to use the data directory on mounted volume"
	chown postgres:postgres /flimeydata
	chmod 700 /flimeydata
	sed -i "s:/var/lib/postgresql/12/main:/flimeydata/main:g" /etc/postgresql/12/main/postgresql.conf
	touch /var/lib/postgresql/reconfigured.txt
fi

echo "start local postgres server"
service postgresql start

if [ ! -f "/flimeydata/alive.txt" ]; then
	echo "flimey databases are not initialized"
	echo "init databases..."
	sudo -u postgres psql -c "create database flimey_data"
	sudo -u postgres psql -c "create database flimey_session"
	sudo -u postgres psql -c "create user flimey_sys with encrypted password 'flimey_sys'"
	sudo -u postgres psql -c "grant all privileges on database flimey_data to flimey_sys"
	sudo -u postgres psql -c "grant all privileges on database flimey_session to flimey_sys"
	echo "database and flimey_sys user initialized"
	touch /flimeydata/alive.txt
fi

echo "load postgres config for flimey"
DB_CONF=$(find . -name "db.conf" -print)
echo $DBCONF

if [ -z "$DB_CONF" ]; then
	echo "db.conf needs to be created"

	DB_CONF_TEMPLATE=$(find . -name "db.template.conf" -print)
	NEW_CONF="${DB_CONF_TEMPLATE%db.template.conf}db.conf"
	touch $NEW_CONF

	echo $'data_url = "jdbc:postgresql://localhost:5432/flimey_data"\n' >> $NEW_CONF
	echo $'data_user = "flimey_sys"\n' >> $NEW_CONF
	echo $'data_password = "flimey_sys"\n' >> $NEW_CONF
	echo $'session_url = "jdbc:postgresql://localhost:5432/flimey_session"\n' >> $NEW_CONF
	echo $'session_user = "flimey_sys"\n' >> $NEW_CONF
	echo $'session_password = "flimey_sys"\n' >> $NEW_CONF

	echo "db.conf created"
fi

echo "generate the application session secret"

EXT_RUNTIME_CONF=$(find . -name "runtime.conf" -print)
echo $EXT_RUNTIME_CONF
if [ ! -z "$EXT_RUNTIME_CONF" ]; then
	echo "delete old session secret - all users must renew their logins"
	rm $EXT_RUNTIME_CONF
fi

RUNTIME_CONF="./runtime.conf"
SECRET_KEY=$(head /dev/urandom | tr -dc A-Za-z0-9 | head -c64)
touch $RUNTIME_CONF
echo $'include "application"\n' >> $RUNTIME_CONF
echo 'play.http.secret.key="'${SECRET_KEY}'"' >> $RUNTIME_CONF

echo "configure default ports"
export PLAY_HTTP_PORT="9080"
export PLAY_HTTPS_PORT="9443"

find . -name 'RUNNING_PID' -delete
echo "starting the server..."

# the docker build script will add the run server line below:

