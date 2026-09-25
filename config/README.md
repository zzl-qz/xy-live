# Local configuration

Copy `application-local.example.yml` to `application-local.yml`, fill in local credentials, and start the service with:

```text
--spring.config.additional-location=optional:file:./config/application-local.yml
```

The real local file is ignored by Git. Do not put production credentials in tracked source files, test resources, logs, or documentation.

The ShardingSphere datasource file and the ID generator datasource also support the `XY_*` environment variables documented in the root `.env.example`. Load the copied `.env` in the shell, IDE run configuration, or Docker Compose before starting services.
