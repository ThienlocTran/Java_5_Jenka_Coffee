# Jenka Coffee Environment Guide

## Local Development

- Profile: `local`
- Database: Neon.tech test PostgreSQL
- Purpose: test create/update/delete safely without touching the real VPS database

Set these environment variables for local runs:

```properties
SPRING_PROFILES_ACTIVE=local
LOCAL_DATABASE_URL=jdbc:postgresql://<neon-host>/<database>?sslmode=require
LOCAL_DATABASE_USERNAME=<neon-user>
LOCAL_DATABASE_PASSWORD=<neon-password>
```

IntelliJ setup:

1. Open `Run/Debug Configurations`
2. Select the Spring Boot backend configuration
3. Add `SPRING_PROFILES_ACTIVE=local` in `Environment variables`
4. Or use VM options: `-Dspring.profiles.active=local`

When local starts, the log must show:

```text
The following 1 profile is active: "local"
```

If local shows `production`, stop immediately.

## Production

- Profile: `production`
- Database: real PostgreSQL on the VPS
- Purpose: real customer data

Production backend must receive configuration from environment variables:

```properties
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/jenka_coffee
SPRING_DATASOURCE_USERNAME=jenka_user
SPRING_DATASOURCE_PASSWORD=<production-password>
```

Production keeps `spring.jpa.hibernate.ddl-auto=validate` so schema is not changed automatically.

## Safety Rules

- Do not run local with profile `production`
- Do not use the production database for testing
- Do not commit secrets to Git
- Do not run `docker compose down -v`
- Do not hardcode production database credentials in source code
