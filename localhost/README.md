# Localhost Environment

Docker Compose setup for running RedFox and its dependencies locally.

## Prerequisites

- Docker with Compose plugin

## Profiles

Services are organized into profiles so you can start only what you need.

| Profile      | Services                            |
|--------------|-------------------------------------|
| _(default)_  | postgres, kafka, kafka-init-job     |
| `monitoring` | prometheus, loki, promtail, grafana |

## Usage

Start infrastructure only (for local development against the IDE):

```shell
docker compose up
```

Start everything including the application containers:

```shell
docker compose --profile apps up
```

Start with full monitoring stack:

```shell
docker compose --profile apps --profile monitoring up
```

Stop and remove all containers:

```shell
docker compose --profile apps --profile monitoring down
```

Remove all containers **and volumes** (resets all data):

```shell
docker compose --profile apps --profile monitoring down -v
```

## Services

### Infrastructure (always started)

| Service      | Port   | Description                                                                |
|--------------|--------|----------------------------------------------------------------------------|
| **postgres** | `5432` | PostgreSQL database (`redfox` database, credentials `postgres`/`postgres`) |

### monitoring (`monitoring` profile)

| Service        | Port   | Description                                                                |
|----------------|--------|----------------------------------------------------------------------------|
| **prometheus** | `9090` | Scrapes metrics from `redfox-app` at `/actuator/prometheus` every 15 s     |
| **loki**       | `3100` | Log aggregation backend                                                    |
| **promtail**   | -      | Discovers Docker containers, parses JSON ECS logs, and ships them to Loki  |
| **grafana**    | `3000` | Dashboards and log exploration (anonymous admin access, no login required) |

Grafana is pre-provisioned with two datasources:

- **Prometheus** (default) - for metrics
- **Loki** - for logs
