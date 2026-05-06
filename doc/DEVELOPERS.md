# Developer guide

## Install PostgreSQL

For RPM based distributions such as Fedora and RHEL you can add the
[PostgreSQL YUM repository](https://yum.postgresql.org/) and do the install via

**Fedora 43**

```sh
rpm -Uvh https://download.postgresql.org/pub/repos/yum/reporpms/F-43-x86_64/pgdg-redhat-repo-latest.noarch.rpm
```

**RHEL 10.x / Rocky Linux 10.x**

**x86_64**

```sh
dnf install https://dl.fedoraproject.org/pub/epel/epel-release-latest-10.noarch.rpm
rpm -Uvh https://download.postgresql.org/pub/repos/yum/reporpms/EL-10-x86_64/pgdg-redhat-repo-latest.noarch.rpm
dnf config-manager --set-enabled crb
```

**aarch64**

```sh
dnf install https://dl.fedoraproject.org/pub/epel/epel-release-latest-10.noarch.rpm
rpm -Uvh https://download.postgresql.org/pub/repos/yum/reporpms/EL-10-aarch64/pgdg-redhat-repo-latest.noarch.rpm
dnf config-manager --set-enabled crb
```

**PostgreSQL 18**

``` sh
dnf -qy module disable postgresql
dnf install -y postgresql18 postgresql18-server postgresql18-contrib postgresql18-libs
```

This will install PostgreSQL 18.

## Install billetsys

### Pre-install

#### Basic dependencies

* [OpenJDK 25](https://openjdk.org/)
* [Apache Maven](https://maven.apache.org/)

#### Generate the manual

1. Download dependencies

``` sh
dnf install pandoc texlive-scheme-basic
```

2. Download Eisvogel

    Use the command `pandoc --version` to locate the user data directory. On Fedora systems, this directory is typically located at `$HOME/.local/share/pandoc`.

    Download the `Eisvogel` template for `pandoc`, please visit the [pandoc-latex-template](https://github.com/Wandmalfarbe/pandoc-latex-template) repository. For a standard installation, you can follow the steps outlined below.

```sh
    wget https://github.com/Wandmalfarbe/pandoc-latex-template/releases/download/v3.4.0/Eisvogel-3.4.0.tar.gz
    tar -xzf Eisvogel-3.4.0.tar.gz
    mkdir -p $HOME/.local/share/pandoc/templates
    mv Eisvogel-3.4.0/eisvogel.latex $HOME/.local/share/pandoc/templates/
```

3. Add package for LaTeX

    Download the additional packages required for generating PDF and HTML files.

```sh
    dnf install 'tex(fvextra.sty)' 'tex(footnote.sty)' 'tex(footnotebackref.sty)' 'tex(pagecolor.sty)' 'tex(hardwrap.sty)' 'tex(mdframed.sty)' 'tex(sourcesanspro.sty)' 'tex(ly1enc.def)' 'tex(sourcecodepro.sty)' 'tex(titling.sty)' 'tex(csquotes.sty)' 'tex(zref-abspage.sty)' 'tex(needspace.sty)' 'tex(selnolig.sty)'
```

4. Generate the manual

```sh
cd doc/manual/en
./generate-manual.sh
```

This creates both `build/manual.html` and `build/manual.pdf`. If the Eisvogel template is stored in a non-standard location, set `EISVOGEL_TEMPLATE` before running the script.

### Build

#### PostgreSQL

In many installations, there is already an operating system user named `postgres` that is used to run the PostgreSQL server. You can use the command

``` sh
getent passwd | grep postgres
```

to check if your OS has a user named postgres. If not use

``` sh
useradd -ms /bin/bash postgres
passwd postgres
```

If the postgres user already exists, don't forget to set its password for convenience.

Open a new window, switch to the `postgres` user. This section will always operate within this user space.

``` sh
sudo su -
su - postgres
```

**Initialize cluster**

If you use dnf to install your postgresql, chances are the binary file is in `/usr/pgsql-18/bin/`

```sh
export PATH=/usr/pgsql-18/bin:$PATH
initdb -k /tmp/pgsql
```

**Remove default access**

Remove last lines from `/tmp/pgsql/pg_hba.conf`

``` ini
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust
host    replication     all             127.0.0.1/32            trust
host    replication     all             ::1/128                 trust
```

**Add access for users and a database**

Add new lines to `/tmp/pgsql/pg_hba.conf`

``` ini
host    ticketdb         ticketdb        127.0.0.1/32            scram-sha-256
host    ticketdb         ticketdb        ::1/128                 scram-sha-256
```

**Start PostgreSQL**

``` sh
pg_ctl  -D /tmp/pgsql/ start
```

Here, you may encounter issues such as the port being occupied or permission being denied. If you experience a failure, you can go to `/tmp/pgsql/log` to check the reason.

You can use

``` sh
pg_isready
```

to test

**Add users and a database**

``` sh
export PATH=/usr/pgsql-18/bin:$PATH
createuser -P ticketdb
createdb -E UTF8 -O ticketdb ticketdb
```

**Verify access**

For the user `ticketdb` (standard) use `ticketdb`

``` sh
psql -h localhost -p 5432 -U ticketdb ticketdb
\q
```

#### billetsys

``` sh
cd /usr/local
git clone https://github.com/mnemosyne-systems/billetsys.git
cd billetsys
```

See `BUILDING.md` for the current development, build, test, and frontend integration commands.

## Logging levels

| Level | Description |
| :------- | :------ |
| TRACE | Information for developers including values of variables |
| DEBUG | Higher level information for developers - typically about flow control and the value of key variables |
| INFO | A user command was successful or general health information about the system |
| WARN | A user command didn't complete correctly so attention is needed |
| ERROR | Something unexpected happened - try to give information to help identify the problem |
| FATAL | We can't recover - display as much information as we can about the problem and `exit(1)` |

## Basic git guide

Here are some links that will help you

* [How to Squash Commits in Git](https://www.git-tower.com/learn/git/faq/git-squash)
* [ProGit book](https://github.com/progit/progit2/releases)

### Start by forking the repository

This is done by the "Fork" button on GitHub.

### Clone your repository locally

This is done by

```sh
git clone git@github.com:<username>/billetsys.git
```

### Add upstream

Do

```sh
cd billetsys
git remote add upstream https://github.com/mnemosyne-systems/billetsys.git
```

### Do a work branch

```sh
git checkout -b mywork main
```

### Make the changes

Remember to verify the compile and execution of the code.

For frontend changes, run the shared checks from `src/frontend` before opening a pull request:

```sh
npm ci
npm run fix
npm run check
```

If you prefer to stay at the repository root, use:

```sh
npm --prefix src/frontend run fix
npm --prefix src/frontend run check
```

If you only need formatting without the ESLint auto-fix pass, run:

```sh
npm run format
```

### AUTHORS

Remember to add your name to the following files,

```
AUTHORS
doc/manual/en/97-acknowledgement.md
```

in your first pull request

### Multiple commits

If you have multiple commits on your branch then squash them

``` sh
git rebase -i HEAD~2
```

for example. It is `p` for the first one, then `s` for the rest

### Rebase

Always rebase

``` sh
git fetch upstream
git rebase -i upstream/main
```

### Force push

When you are done with your changes force push your branch

``` sh
git push -f origin mywork
```

and then create a pull requests for it

### Repeat

Based on feedback keep making changes, squashing, rebasing and force pushing

### PTAL

When you are working on a change put it into Draft mode, so we know that you are not
happy with it yet.

Please, send a PTAL to the Committer that were assigned to you once you think that
your change is complete. And, of course, take it out of Draft mode.

### Undo

Normally you can reset to an earlier commit using `git reset <commit hash> --hard`.
But if you accidentally squashed two or more commits, and you want to undo that,
you need to know where to reset to, and the commit seems to have lost after you rebased.

But they are not actually lost - using `git reflog`, you can find every commit the HEAD pointer
has ever pointed to. Find the commit you want to reset to, and do `git reset --hard`.
