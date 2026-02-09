# billetsys: A Modern Support Ticket solution

`billetsys` is a modern support ticket solution that aims to be easy for all roles to
navigate and get their work done quicker.

## Features

* Support ticket systems with 4 roles (User, TAM, Support, Admin)

## Technologies

* [Java 17+](https://openjdk.org/)
* [Maven](https://maven.apache.org/)
* [Quarkus](https://quarkus.io/)
* [PostgreSQL](https://www.postgresql.org)

## Developer

Setup PostgreSQL with a user with `ticketdb`/`ticketdb` as username and password.

``` bash
git clone https://github.com/mnemosyne-systems/billetsys.git
cd billetsys
dropdb ticketdb
createdb -E UTF8 -O ticketdb ticketdb
mvn clean quarkus:dev
```

The users defined for testing are

* User: `user1` / `user1`
* User: `user2` / `user2`
* TAM: `tam` / `tam`
* Support: `support1` / `support1`
* Support: `support2` / `support2`
* Admin: `admin` / `admin`

## Contributing

Contributions to `pgopr` are managed on [GitHub.com](https://github.com/mnemosyne-systems/billetsys/)

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
