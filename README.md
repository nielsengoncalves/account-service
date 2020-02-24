# Account-Service
Simple backend service with money operation APIs backed by [Ktor](https://ktor.io/).

## Requirements
- Java 8

## Build & Running
The following command can be used to build the project:

```bash
$ ./gradlew build
```

The executable jar will be available in the folder `/build/libs` and can be executed with the following command:

```bash
$ java -jar money-transfer-0.0.1.jar
```

By default the server will start listening in the port `8080`, but it can be changed in the `resources/application.conf`

## API Definition

### Create Account
```
$ curl -X POST 'http://localhost:8080/accounts' -i \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -d '{"id": "bd3e5a04-71fc-4588-baf6-809269842c23"}'
```

Response:
```
HTTP/1.1 201 Created
Content-Length: 60
Content-Type: application/json; charset=UTF-8

{"id":"bd3e5a04-71fc-4588-baf6-809269842c23","balance":0.00}
```

### Find Account (Idempotent)
```
$ curl -X GET 'http://localhost:8080/accounts/bd3e5a04-71fc-4588-baf6-809269842c23' -i \
    -H 'Accept: application/json'
```

Response:
```
HTTP/1.1 200 OK
Content-Length: 60
Content-Type: application/json; charset=UTF-8

{"id":"bd3e5a04-71fc-4588-baf6-809269842c23","balance":0.00}
```

### Deposit (Idempotent)
```
$ curl -X POST 'http://localhost:8080/accounts/bd3e5a04-71fc-4588-baf6-809269842c23/deposit' -i \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -d '{"amount": "1000.00", "deduplicationId": "8da35d0c-a56f-40b6-8985-240f7fcc280f"}'
```

Response:
```
HTTP/1.1 200 OK
Content-Length: 63
Content-Type: application/json; charset=UTF-8

{"id":"bd3e5a04-71fc-4588-baf6-809269842c23","balance":1000.00}
```

### Withdraw (Idempotent)
```
$ curl -X POST 'http://localhost:8080/accounts/bd3e5a04-71fc-4588-baf6-809269842c23/withdraw' -i \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -d '{"amount": 250.00, "deduplicationId": "21a3173c-03f3-43ff-b2df-568a676b7b0e"}'
```

Response:
```
HTTP/1.1 200 OK
Content-Length: 62
Content-Type: application/json; charset=UTF-8

{"id":"bd3e5a04-71fc-4588-baf6-809269842c23","balance":750.00}
```

### Transfer (Idempotent)
```
$ curl -X POST 'http://localhost:8080/accounts/bd3e5a04-71fc-4588-baf6-809269842c23/transfer' -i \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -d '{"destinationAccountId": "e7bf11c8-0d10-40cc-b4ff-a2690017d965", "amount": "100.00", "deduplicationId": "9a7c361b-fec6-441f-9f99-d34e91e58b65"}'
```

Response:
```
HTTP/1.1 200 OK
Content-Length: 62
Content-Type: application/json; charset=UTF-8

{"id":"bd3e5a04-71fc-4588-baf6-809269842c23","balance":650.00}
```

## To improve:
- Add [swagger](https://swagger.io/) for API documentation.
- Add logs, use MDC