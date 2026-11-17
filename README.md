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

