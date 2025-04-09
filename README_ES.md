# Proyecto PetConnect 

[![English Version](https://img.shields.io/badge/Version-English-blue)](README.md)

Plataforma digital integral para la gestión de la salud animal, incluyendo certificación digital de vacunación, historiales médicos, citas online y comunicación veterinaria. Este monorepo contiene el backend (Java/Spring Boot) y el frontend (React/TypeScript).

<!-- TODO: Añadir captura de pantalla de la aplicación funcionando aquí -->
<!-- ![Captura Aplicación PetConnect](.github/readme-assets/app-screenshot.png) -->

## 1. Prerrequisitos

Asegúrate de tener instalado el siguiente software en tu sistema:

*   **Git:** Para clonar el repositorio y gestionar versiones ([https://git-scm.com/](https://git-scm.com/)).
*   **Java JDK 21:** O una versión compatible ([https://adoptium.net/](https://adoptium.net/)). Verifica con `java -version`.
*   **Apache Maven:** Para construir el proyecto backend ([https://maven.apache.org/](https://maven.apache.org/)). Verifica con `mvn -version`. Asegúrate de que esté añadido al PATH de tu sistema.
*   **Node.js & npm:** Para gestionar las dependencias y ejecutar el proyecto frontend (Se recomienda la versión LTS - [https://nodejs.org/](https://nodejs.org/)). Verifica con `node -v` y `npm -v`.
*   **Docker & Docker Compose:** Para ejecutar los servicios contenedorizados (Base de Datos, SonarQube) ([https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)). Verifica con `docker --version` y `docker-compose --version` (o `docker compose version`).

**Herramientas Recomendadas:**

*   Un IDE como IntelliJ IDEA o VS Code para editar código y depurar.
*   Postman o una herramienta similar para probar la API ([https://www.postman.com/downloads/](https://www.postman.com/downloads/)).

## 2. Clonar el Repositorio


```bash
git clone https://github.com/i-bosquet/petconnect.git
cd petconnect
```

## 3. Configuración Inicial: Variables de Entorno
Antes de ejecutar la aplicación, necesitas configurar las variables de entorno.

- **Crear archivo `.env`**: Este archivo **NO** está incluido en el repositorio por seguridad. Debes crearlo manualmente en la raíz del proyecto (`petconnect/`) con el siguiente contenido exacto:
```bash
# --- Configuración Base de Datos ---
# Usado por Docker Compose y la Aplicación Backend (vía variables de entorno)
POSTGRES_DB=petconnect_db
POSTGRES_USER=root
POSTGRES_PASSWORD=1234 # ¡Cambia esto si quieres una contraseña más segura!

# --- Análisis SonarQube ---
# Usado por los scripts de análisis (run-sonar-analysis.ps1 / .sh)
# Genera un token en tu instancia de SonarQube (http://localhost:9000)
# Ve a: Administration -> Security -> Users -> Tokens (para tu usuario) -> Generate Tokens
SONAR_TOKEN=TU_TOKEN_DE_SONARQUBE_AQUI
```
> [!IMPORTANT]
> Reemplaza `TU_TOKEN_DE_SONARQUBE_AQUI` con un token real generado desde tu instancia local de SonarQube una vez que esté en ejecución (ver Paso 4).

## 4. Iniciar los Servicios Contenerizados
Abre una terminal en la raíz del proyecto (`petconnect/`) y ejecuta:

```bash
docker-compose up -d
```

Este comando descargará las imágenes necesarias (si no las tienes) y creará e iniciará los siguientes contenedores en segundo plano (`-d`):
- `petconnect_db`: Contenedor con la base de datos PostgreSQL 17.4.
- `petconnect_adminer`: Interfaz web para gestionar la base de datos.
- `petconnect_sonarqube`: Contenedor con el servidor SonarQube.

Puedes verificar que los contenedores están corriendo con:
```bash
docker ps
```
<!-- TODO: Añadir captura de 'docker ps -->
<!-- ![Contenedores Docker Corriendo](.github/readme-assets/docker-ps.png) -->

## 5. Construir el Backend
Abre una terminal en la raíz del proyecto (`petconnect/`) y navega al directorio del backend. 
Luego, construye el proyecto usando Maven:

```bash
cd backend
mvn clean install -DskipTests 
# -DskipTests es opcional, acelera la construcción si no necesitas ejecutar tests aún
# En construcciones posteriores, 'mvn package' podría ser suficiente si no han cambiado las dependencias
cd .. 
# Vuelve al directorio raíz
```

Esto compilará el código Java, descargará dependencias y empaquetará la aplicación (normalmente como un archivo JAR en el directorio `backend/target`).

## 6. Ejecutar el Backend (Modo Desarrollo)
Asegúrate de que los contenedores Docker (especialmente la base de datos `petconnect_db`) estén en ejecución (Paso 4).
- **Usando el Plugin Maven de Spring Boot (Recomendado para Terminal):**
  Abre una terminal en el directorio `petconnect/backend/` y ejecuta:
```bash
mvn spring-boot:run
```

La aplicación Spring Boot se iniciará, se conectará a la base de datos en el contenedor y estará disponible en `http://localhost:8080`
 
- **Usando un IDE (Alternativa):**
  - Importa la carpeta `backend` como un proyecto Maven en tu IDE preferido (IntelliJ IDEA, Eclipse, VS Code con extensiones Java).
  - Localiza la clase `com.petconnect.backend.BackendApplication`.
  - Ejecuta esta clase directamente desde el IDE.

## 7. Ejecutar el Frontend (Modo Desarrollo)

*   Abre una terminal en el directorio `petconnect/frontend/`.
*   **Instala dependencias (solo necesario la primera vez o tras actualizaciones):**
    ```bash
    npm install
    ```
*   **Inicia el servidor de desarrollo:**
    ```bash
    npm run dev
    ```
*   Vite iniciará el servidor de desarrollo. Abre tu navegador web en la URL indicada (normalmente `http://localhost:5173`).

## 8. Acceder a las Herramientas y Aplicación

Una vez todo esté en ejecución:

*   **Aplicación Frontend:** http://localhost:5173 (o el puerto que indique Vite)
*   **URL Base API Backend:** http://localhost:8080
*   **Swagger UI (Docs API):** http://localhost:8080/swagger-ui.html
*   **Servidor SonarQube:** http://localhost:9000 (Login inicial: admin / admin - ¡Cámbialo!)
*   **Adminer (Gestión BD):** http://localhost:8081
    *   Sistema: `PostgreSQL`
    *   Servidor: `petconnect_db` (Nombre del servicio/contenedor)
    *   Usuario: `root` (de tu archivo `.env`)
    *   Contraseña: `1234` (de tu archivo `.env`)
    *   Base de datos: `petconnect_db` (de tu archivo `.env`)

<!-- TODO: Añadir capturas de Swagger UI, SonarQube, Adminer login -->
<!-- ![Swagger UI](.github/readme-assets/swagger-ui.png) -->
<!-- ![Dashboard SonarQube](.github/readme-assets/sonarqube.png) -->
![Adminer Login](.github/readme-assets/adminer.png)

## 9. Ejecutar Análisis con SonarQube

- Asegúrate de haber generado un token en SonarQube (http://localhost:9000) y haberlo añadido a tu archivo `.env` (Paso 3).
- Abre una terminal en la raíz del proyecto (`petconnect/`).
- Navega al directorio `scripts/`: `cd scripts`
- Ejecuta el script apropiado para tu sistema operativo:
    - **Windows (PowerShell):** `.\run-sonar-analysis.ps1`
    - **Linux / macOS (Bash):** `bash run-sonar-analysis.sh` (o `./run-sonar-analysis.sh` tras `chmod +x run-sonar-analysis.sh`)
- Maven construirá el proyecto, ejecutará los tests (generando reportes Jacoco) y enviará los resultados al servidor SonarQube. Podrás ver el análisis en `http://localhost:9000`.
- [![Quality Gate Status](http://localhost:9000/api/project_badges/measure?project=petconnect_backend&metric=alert_status&token=sqa_761d07dc5cd239cee77d9ee6bcc7d4391b39f53f)](http://localhost:9000/dashboard?id=petconnect_backend&codeScope=overall)

## 10. Probar la API con Postman

-   Abre Postman.
-   Importa la colección: "Archivo" > "Importar..." y selecciona el archivo `postman/PetConnect.postman_collection.json`.
-   Importa el entorno: "Archivo" > "Importar..." y selecciona el archivo `postman/PetConnect_Local_Dev.postman_environment.json`.
-   Asegúrate de que el entorno "PetConnect Local Dev" está seleccionado en la esquina superior derecha.
-   Ahora puedes explorar las carpetas y ejecutar las peticiones contra tu backend local (http://localhost:8080).

<!-- TODO: Añadir captura de Postman con la colección/entorno importado -->
<!-- ![Configuración Postman](.github/readme-assets/postman-setup.png) -->

---
*For English instructions, please see [README.md](README.md).*