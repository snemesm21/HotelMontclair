# Hotel Montclair

Aplicación web para la gestión y presentación de un hotel. El proyecto está construido con Spring Boot, Thymeleaf, Spring Data JPA y la base de datos H2.

## Tecnologías

- Java 17
- Spring Boot 4.1.1
- Spring MVC
- Thymeleaf
- Spring Data JPA / Hibernate
- H2 Database
- Lombok
- Maven Wrapper
- HTML, CSS y JavaScript
- Swiper.js (para carruseles dinámicos)

## Requisitos

- JDK 17 o superior
- Windows PowerShell, CMD o una terminal compatible

El proyecto incluye Maven Wrapper, por lo que no es necesario instalar Maven manualmente.

## Ejecutar el proyecto

En Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

También puedes ejecutar la clase `DemoApplication` desde VS Code.

La aplicación queda disponible en:

```text
http://localhost:8080
```

Para detenerla, presiona `Ctrl + C` en la terminal donde está ejecutándose.

## Base de datos H2 y JPA

La configuración está en `src/main/resources/application.properties`.

- URL JDBC: `jdbc:h2:mem:hotelmontclair`
- Usuario: `sa`
- Contraseña: vacía
- Consola H2: `http://localhost:8080/h2-console`
- URL de conexión en H2: `jdbc:h2:mem:hotelmontclair`
- `spring.jpa.hibernate.ddl-auto=create-drop`

La base de datos funciona en memoria. Por eso las tablas y los datos se crean nuevamente cada vez que se inicia la aplicación y se eliminan al detenerla.

Las entidades JPA principales son:

- `Client`
- `RoomType`
- `Room`
- `Service`

La relación entre habitación y tipo de habitación es `ManyToOne`: muchas habitaciones pueden pertenecer a un mismo tipo.

## Datos iniciales

Los datos de prueba se cargan automáticamente desde `DataInitializer`:

- 10 clientes
- 5 tipos de habitación
- 50 habitaciones
- 8 servicios del hotel

Cada habitación queda asociada a un `RoomType` y cuenta con imágenes, descripción y estado. Las habitaciones públicas se muestran como catálogo; el inventario completo se consulta desde la sección administrativa.

## Credenciales de prueba

Administrador:

```text
Usuario: admin
Contraseña: admin
```

Cliente de prueba:

```text
Usuario: demo
Contraseña: demo
```

También se generan usuarios desde `cliente3` hasta `cliente10`. Su contraseña coincide con su usuario.

## Rutas principales

### Sitio público

| Ruta | Descripción |
| --- | --- |
| `/` | Página principal |
| `/rooms/cards` | Catálogo público de habitaciones |
| `/rooms/1` | Detalle de una habitación |
| `/services` | Catálogo de servicios |
| `/services/1` | Detalle de un servicio |
| `/login` | Inicio de sesión |
| `/profile/{id}` | Perfil de un cliente |

Los detalles de servicios y habitaciones incluyen imágenes, descripciones, características y galerías.

### Administración

| Ruta | Descripción |
| --- | --- |
| `/admin/rooms` | Inventario completo de habitaciones |
| `/admin/rooms/add` | Crear una habitación |
| `/admin/rooms/edit/{id}` | Editar una habitación |
| `/admin/rooms/delete/{id}` | Eliminar una habitación |
| `/admin/room-types` | Administrar tipos de habitación |
| `/admin/room-types/add` | Crear un tipo de habitación |
| `/clients` | Administrar clientes |
| `/services/table` | Servicios en vista de tabla |
| `/services/add` | Crear un servicio |
| `/services/update/{id}` | Editar un servicio |
| `/services/delete/{id}` | Eliminar un servicio |

## Estructura del proyecto

```text
src/main/java/com/example/demo/
├── DemoApplication.java
├── DataInitializer.java
├── controller/       Controladores web
├── entities/         Entidades JPA y modelos del dominio
├── repository/       Interfaces JpaRepository
├── service/          Servicios y lógica de aplicación
└── errors/           Excepciones personalizadas (ej. NotFoundException)

src/main/resources/
├── application.properties
├── static/
│   ├── css/
│   ├── Images/
│   └── js/
└── templates/        Vistas Thymeleaf
```

## Pruebas

Ejecutar todas las pruebas:

```powershell
.\mvnw.cmd test
```

Compilar sin ejecutar las pruebas:

```powershell
.\mvnw.cmd clean package -DskipTests
```

## Manejo de errores

Los errores de las vistas web (como entidades no encontradas en la base de datos) se gestionan de forma explícita mediante bloques `try-catch` en cada controlador, capturando excepciones personalizadas como `NotFoundException` y pasando el mensaje a la plantilla de error. La plantilla correspondiente se encuentra en:

```text
src/main/resources/templates/error.html
```

## Notas

- Las imágenes del contenido inicial usan URLs externas de Unsplash.
- La consola H2 solo está pensada para desarrollo.
- Para conservar datos entre reinicios habría que cambiar H2 en memoria por una base persistente y ajustar `ddl-auto`.
