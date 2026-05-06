#
# Eclipse Public License - v 2.0
#
# THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
# PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
# OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
#

.PHONY: all format clean run test docs db-drop db-create full frontend platform

all: clean format run

frontend:
	@if [ ! -f src/frontend/node_modules/.package-lock.json ] || \
		[ src/frontend/package.json -nt src/frontend/node_modules/.package-lock.json ] || \
		[ src/frontend/package-lock.json -nt src/frontend/node_modules/.package-lock.json ]; then \
		npm --prefix src/frontend ci; \
	fi

format: frontend
	@npm --prefix src/frontend run fix -q
	@npm --prefix src/frontend run check -q

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

platform:
	@if command -v podman-compose >/dev/null 2>&1; then \
		podman-compose up -d; \
	elif command -v docker >/dev/null 2>&1; then \
		docker compose up -d; \
	else \
		echo "No container runtime found (podman-compose or docker). Skipping compose services."; \
	fi

full: db-drop db-create platform clean frontend format test docs run
