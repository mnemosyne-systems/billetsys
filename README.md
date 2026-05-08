# billetsys: Modern Support Ticket Solution

[![License: EPL-2.0](https://img.shields.io/badge/License-EPL_2.0-red.svg)](https://www.eclipse.org/legal/epl-2.0/)
[![Latest release](https://img.shields.io/github/v/release/mnemosyne-systems/billetsys)](https://github.com/mnemosyne-systems/billetsys/releases)
[![GitHub stars](https://img.shields.io/github/stars/mnemosyne-systems/billetsys?style=social)](https://github.com/mnemosyne-systems/billetsys/stargazers)
[![Discussions](https://img.shields.io/github/discussions/mnemosyne-systems/billetsys)](https://github.com/mnemosyne-systems/billetsys/discussions)

<p align="center">
  <img src="doc/logo/logo.svg" alt="billetsys logo" width="256"/>
</p>

[English](https://github.com/mnemosyne-systems/billetsys/releases/latest/download/billetsys-en.pdf)

**billetsys** is a modern support ticket solution that aims to be easy for all
roles to navigate and get their work done quicker.

## Features

- Support ticket system with 5 roles (User, Superuser, TAM, Support, Admin)
- Branding
- Public and private messages
- Search across ticket numbers and messages
- Ticket cross-references and mentions
- Article cross-references and mentions
- Ticket shortcuts
- Email integration with per-user format preferences
- CSV ticket import
- Markdown editor with rich content support
- Light and dark mode
- Captcha support, including for forgot-password flows
- Session inactivity handling
- Transport Layer Security v1.3 (TLS) support

## Documentation

- [Developer guide](https://github.com/mnemosyne-systems/billetsys/blob/main/doc/DEVELOPERS.md)
- [Build and run guide](https://github.com/mnemosyne-systems/billetsys/blob/main/doc/BUILDING.md)
- [Frontend guide](https://github.com/mnemosyne-systems/billetsys/blob/main/src/frontend/README.md)

See the [releases page](https://github.com/mnemosyne-systems/billetsys/releases)
for downloads and release notes.

## Technologies

**billetsys** is built with

- [Quarkus](https://quarkus.io/) on [Java 25](https://openjdk.org/) for the backend
- [PostgreSQL](https://www.postgresql.org) for storage
- [React](https://react.dev/) and [TypeScript](https://www.typescriptlang.org/)
  on the frontend, bundled with [Vite](https://vitejs.dev/)
- [shadcn/ui](https://ui.shadcn.com/) components with [Tailwind CSS](https://tailwindcss.com/)
- [Red Hat Text](https://github.com/RedHatOfficial/RedHatFont) as the default typeface
- [Maven](https://maven.apache.org/) and [make](https://www.gnu.org/software/make/) for builds
- [Node.js and npm](https://nodejs.org/) for frontend dependencies

## Getting started

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

This detects whether you have Podman Compose or Docker Compose and starts the
containers defined in `docker-compose.yml`. If neither is installed, the command
is skipped with a warning.

Start billetsys in development mode:

```sh
make
```

The application is available at <http://localhost:8080>.

### Test accounts

The users defined for testing are

- User: `user1` / `user1`
- User: `user2` / `user2`
- User: `userb` / `userb`
- Superuser: `superuser1` / `superuser1`
- Superuser: `superuser2` / `superuser2`
- TAM: `tam1` / `tam1`
- TAM: `tam2` / `tam2`
- Support: `support1` / `support1`
- Support: `support2` / `support2`
- Admin: `admin` / `admin`

## Contributing

Contributions to **billetsys** are managed on [GitHub.com](https://github.com/mnemosyne-systems/billetsys/)

- [Ask a question](https://github.com/mnemosyne-systems/billetsys/discussions)
- [Raise an issue](https://github.com/mnemosyne-systems/billetsys/issues)
- [Feature request](https://github.com/mnemosyne-systems/billetsys/issues)
- [Code submission](https://github.com/mnemosyne-systems/billetsys/pulls)

Contributions are most welcome !

Please, consult our [Code of Conduct](https://github.com/mnemosyne-systems/billetsys/blob/main/CODE_OF_CONDUCT.md)
policies for interacting in our community.

Consider giving the project a [star](https://github.com/mnemosyne-systems/billetsys/stargazers)
on [GitHub](https://github.com/mnemosyne-systems/billetsys/) if you find it useful.

## License

[Eclipse Public License - v2.0](https://www.eclipse.org/legal/epl-2.0/)
