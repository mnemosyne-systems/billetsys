#
# Eclipse Public License - v 2.0
#
# THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
# PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
# OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
#

.PHONY: all format clean run test docs db-drop db-create full frontend

all: clean format run

frontend:
	@if [ ! -f src/frontend/node_modules/.package-lock.json ] || \
		[ src/frontend/package.json -nt src/frontend/node_modules/.package-lock.json ] || \
		[ src/frontend/package-lock.json -nt src/frontend/node_modules/.package-lock.json ]; then \
		npm --prefix src/frontend ci; \
	fi

format: frontend
	@npm run frontend:fix -q
	@npm run frontend:check -q

clean:
	@mvn clean

run:
	@mvn quarkus:dev

test:
	@mvn test

docs:
	@mvn -Pmanual-docs generate-resources

db-drop:
	@dropdb ticketdb

db-create:
	@createdb -E UTF8 -O ticketdb ticketdb

full: db-drop db-create clean frontend format test docs run
