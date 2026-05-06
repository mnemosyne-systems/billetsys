# billetsys: Modern Support Ticket Solution

<p align="center">
  <img src="doc/logo/logo-128.png" alt="billetsys logo" width="128" height="128">
</p>

`billetsys` is a modern support ticket solution that aims to be easy for all roles to
navigate and get their work done quicker.

## Features

* Support ticket system with 5 roles (User, Superuser, TAM, Support, Admin)
* Ticket shortcuts
* Ticket and article references
* Branding
* Email integration
* Markdown editor
* CSV import
* Transport Layer Security v1.3 (TLS) support

## Technologies

* [Java 25](https://openjdk.org/)
* [Maven](https://maven.apache.org/)
* [Node.js and npm](https://nodejs.org/)
* [Quarkus](https://quarkus.io/)
* [PostgreSQL](https://www.postgresql.org)
* [make](https://www.gnu.org/software/make/)
* [Vite](https://vitejs.dev/)
* [React](https://react.dev/)
* [TypeScript](https://www.typescriptlang.org/)

## Developer

See the developer documentation for setup and workflow details:

* [Developer guide](./doc/DEVELOPERS.md)
* [Build and run guide](./doc/BUILDING.md)
* [Frontend guide](./src/frontend/README.md)

### Quick start

Clone the repository:

```sh
git clone https://github.com/mnemosyne-systems/billetsys.git
cd billetsys
```

Set up [PostgreSQL](https://www.postgresql.org/):

```sh
createuser -P ticketdb
createdb -E UTF8 -O ticketdb ticketdb
```

where the password is `ticketdb`. Enable access in `pg_hba.conf` and reload.

The configuration is defined in `src/backend/main/resources/application.properties`.

Copy the example environment file and fill in the secrets:

```sh
cp .env.example .env
```

Start the support services (CAP + Valkey) with compose:

```sh
make platform
```

This detects whether you have Podman Compose or Docker Compose and starts the containers defined
in `docker-compose.yml`. If neither is installed, the command is skipped with a warning.

Start billetsys in development mode:

```sh
make
```

The application is available at http://localhost:8080.

### Test accounts

The users defined for testing are

* User: `user1` / `user1`
* User: `user2` / `user2`
* User: `userb` / `userb`
* Superuser: `superuser1` / `superuser1`
* Superuser: `superuser2` / `superuser2`
* TAM: `tam1` / `tam1`
* TAM: `tam2` / `tam2`
* Support: `support1` / `support1`
* Support: `support2` / `support2`
* Admin: `admin` / `admin`

## Contributing

Contributions to `billetsys` are managed on [GitHub.com](https://github.com/mnemosyne-systems/billetsys/)

* [Ask a question](https://github.com/mnemosyne-systems/billetsys/discussions)
* [Raise an issue](https://github.com/mnemosyne-systems/billetsys/issues)
* [Feature request](https://github.com/mnemosyne-systems/billetsys/issues)
* [Code submission](https://github.com/mnemosyne-systems/billetsys/pulls)

Contributions are most welcome !

Please, consult our [Code of Conduct](./CODE_OF_CONDUCT.md) policies for interacting in our
community.

Consider giving the project a [star](https://github.com/mnemosyne-systems/billetsys/stargazers) on
[GitHub](https://github.com/mnemosyne-systems/billetsys/) if you find it useful.

## License

[Eclipse Public License - v2.0](https://www.eclipse.org/legal/epl-2.0/)
