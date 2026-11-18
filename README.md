\# API de Inventario (Equipo 5)



Arquetipo básico del Sprint 0 — Desarrollo de Software II, Universidad del Valle.



Es una versión inicial que recorre todas las capas con un único caso: \*\*consultar un artículo por su código\*\*.



\## Tecnologías



\- Java 21 y Spring Boot 4.1.2

\- Spring Data JPA (Hibernate) y PostgreSQL 17

\- Flyway (migraciones de la base de datos)

\- Gradle (wrapper incluido)

\- Pruebas con JUnit 5 y Testcontainers

\- Cobertura con JaCoCo, estilo con Checkstyle y análisis con SonarCloud



\## Estructura



| Capa (DAS) | Paquete | Clase |

|---|---|---|

| Vista | `controller` | `ArticuloController` |

| Lógica | `service` | `ArticuloService` |

| Acceso a datos | `repository` | `ArticuloRepository` |

| Modelo | `model` | `Articulo`, `TipoArticulo` |

| Transferencia | `dto` | `ArticuloDto` |

| Configuración | `config` | `OpenApiConfig` |



La tabla `articulo` se crea con la migración `src/main/resources/db/migration/V1\_\_esquema\_inicial.sql`

y los datos de demostración con `V2\_\_datos\_demo.sql`.



\## Endpoint



| Método | Ruta | Respuesta |

|---|---|---|

| GET | `/inventario/articulos/{codigo}` | `200` con el artículo, o `404` si no existe |



Ejemplos: `PT-00001` devuelve 200 y `PT-99999` devuelve 404.



Documentación interactiva (Swagger): http://localhost:8080/swagger-ui.html



\## Ejecutar con Docker



```bash

cp .env.example .env

docker compose up --build

```



En Windows (PowerShell): `Copy-Item .env.example .env`.



Si cambia el esquema de la base de datos (por ejemplo, la migración V1), borre el volumen antes de volver a levantar:



```bash

docker compose down -v --remove-orphans

```



\## Pruebas y cobertura



Se necesita Docker en ejecución, porque las pruebas usan Testcontainers con PostgreSQL.



```bash

./gradlew test

./gradlew integrationTest

./gradlew jacocoTestReport

```



En Windows (PowerShell) use `.\\gradlew.bat` en lugar de `./gradlew`.



Reporte de cobertura: `build/reports/jacoco/test/html/index.html`



El CI exige una cobertura de líneas de al menos 80 %.

## Despliegue continuo en EC2

El workflow se ejecuta manualmente sobre `main`. Elige uno de estos modos:

- `build` (predeterminado) valida Compose y Bash y construye la imagen localmente en el runner.
- `publish` construye y publica la imagen en Docker Hub con el SHA completo del commit como etiqueta.
- `deploy` construye y publica la imagen. Después intenta actualizar solo el backend de EC2 usando el digest SHA-256 de esa imagen.

En GitHub, configura `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN` como secretos del entorno `dockerhub`. En el entorno `production`, configura los secretos `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` y `EC2_KNOWN_HOSTS`. Limita `production` a `main` y exige la aprobación de un revisor antes de habilitar el despliegue con `EC2_DEPLOY_ENABLED=true`.

Antes de ejecutar `deploy`, guarda el Compose y la referencia de imagen anteriores. Instala en `~/despliegue` el `docker-compose.aws.yml` exacto del commit aprobado y comprueba la compatibilidad de Flyway sobre una restauración de la base de datos.

Cuando Compose y Actuator confirman que el backend está saludable, el script reemplaza de forma atómica `.inventario-image.env`. Este archivo guarda el último digest confirmado como `DOCKER_IMAGE=<imagen@sha256:digest>`. Después del primer despliegue saludable, consulta el estado del proyecto en EC2 con este comando:

```bash
cd ~/despliegue
env -u DOCKER_IMAGE docker compose --env-file .env --env-file .inventario-image.env -f docker-compose.aws.yml ps
```

`.inventario-image.env` tiene prioridad sobre `.env`. `env -u DOCKER_IMAGE` evita que una variable exportada en la sesión tenga prioridad sobre ambos archivos. Si el intento falla después de recrear el backend, el archivo conserva el digest anterior. La recuperación del contenedor es manual y requiere una versión de Compose y una imagen compatibles con la base de datos.

### Activación futura desde `main`

1. Tras aprobar el ensayo manual, añade el evento `push` con `branches: [main]` y conserva `workflow_dispatch`.
2. Usa el modo `deploy` para los eventos `push` y `inputs.mode` para las ejecuciones manuales. Aplica ese modo al entorno de construcción, al inicio de sesión en Docker Hub, a la publicación y a la condición del job de despliegue.
3. Mantén la aprobación de `production`, la variable `EC2_DEPLOY_ENABLED` y las comprobaciones de Compose y salud. Hasta automatizar la entrega de Compose, instala en EC2 la versión exacta cada vez que cambie ese archivo, antes de aprobar el job de producción.
