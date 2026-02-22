# Security

## Roles

The default roles installed in the development environment are

* User: `user1` / `user1`
* User: `user2` / `user2`
* TAM: `tam` / `tam`
* Support: `support1` / `support1`
* Support: `support2` / `support2`
* Admin: `admin` / `admin`

## TLS

This application is configured to use HTTPS with TLSv1.3 by default, using Quarkus TLS Registry, while also keeping HTTP enabled.

### Default behavior

- HTTP is enabled on `8080`.
- HTTPS listens on port `8443` by default (`HTTPS_PORT` override supported).
- In dev mode, HTTP/HTTPS bind on `localhost` so local access is `http://localhost:8080` and `https://localhost:8443`.
- TLS protocol is restricted to `TLSv1.3`.
- Default keystore path is `src/main/resources/tls/server-keystore.p12`.

Configured properties:

```properties
quarkus.http.insecure-requests=enabled
quarkus.http.port=${HTTP_PORT:8080}
quarkus.http.ssl-port=${HTTPS_PORT:8443}
quarkus.http.tls-configuration-name=app
quarkus.tls.app.key-store.p12.path=${TLS_KEYSTORE_PATH:src/main/resources/tls/server-keystore.p12}
quarkus.tls.app.key-store.p12.password=${TLS_KEYSTORE_PASSWORD:changeit}
quarkus.tls.app.protocols=TLSv1.3
%dev.quarkus.http.host=localhost
```

With this setup, both of these URLs work:

- `http://localhost:8080`
- `https://localhost:8443`

### Generate self-signed certificates (default development setup)

Generate a PKCS12 keystore compatible with the configured TLS registry:

```bash
mkdir -p src/main/resources/tls
keytool -genkeypair \
  -alias billetsys-local \
  -keyalg RSA -keysize 2048 \
  -sigalg SHA256withRSA \
  -storetype PKCS12 \
  -keystore src/main/resources/tls/server-keystore.p12 \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=localhost, OU=Development, O=mnemosyne-systems, L=Local, ST=Local, C=US" \
  -ext SAN=dns:localhost,ip:127.0.0.1 \
  -validity 3650
```

### Trust the self-signed certificate locally (optional)

Export the public certificate:

```bash
keytool -exportcert \
  -rfc \
  -alias billetsys-local \
  -keystore src/main/resources/tls/server-keystore.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -file src/main/resources/tls/server-cert.pem
```

You can import `src/main/resources/tls/server-cert.pem` into your browser/OS trust store for local development.

### Runtime overrides

Use environment variables to replace defaults:

- `HTTPS_PORT`
- `HTTP_HOST` (set `%dev.quarkus.http.host=${HTTP_HOST:localhost}` if you want a configurable host binding)
- `TLS_KEYSTORE_PATH`
- `TLS_KEYSTORE_PASSWORD`

Example:

```bash
HTTPS_PORT=9443 TLS_KEYSTORE_PATH=/etc/billetsys/server.p12 TLS_KEYSTORE_PASSWORD='strong-password' ./mvnw quarkus:dev
```

### Disabling one protocol

#### Disable HTTP (HTTPS only)

```properties
quarkus.http.insecure-requests=disabled
```

#### Disable HTTPS (HTTP only)

```properties
quarkus.http.ssl-port=0
```

If you disable HTTPS, `quarkus.http.tls-configuration-name` and `quarkus.tls.*` are not used.

### Avoiding `SSL_ERROR_RX_RECORD_TOO_LONG`

- Use `http://localhost:8080` for HTTP.
- Use `https://localhost:8443` for HTTPS.
- Do **not** use `https://localhost:8080` (plain HTTP port), which causes `SSL_ERROR_RX_RECORD_TOO_LONG`.

### Test profile behavior

Tests keep HTTP enabled (`%test.quarkus.http.insecure-requests=enabled`) so existing test routes continue
to work without HTTPS redirects.

---

Reference basis: [Quarkus TLS Registry](https://quarkus.io/guides/tls-registry-reference) configuration model
(`quarkus.tls.*`) from the TLS registry reference guide.
