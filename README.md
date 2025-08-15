# JPlag Microservice

Microservicio para detección de plagio en código fuente usando JPlag 6.2.0. Este servicio recibe solicitudes del API Gateway para analizar entregas de proyectos Java y detectar similitudes que puedan indicar plagio.

## Características

- ✅ Análisis de plagio usando JPlag 6.2.0
- ✅ Soporte para proyectos Java
- ✅ Clonado automático de repositorios Git
- ✅ Generación de reportes HTML navegables
- ✅ API REST para integración con API Gateway
- ✅ Servir reportes HTML estáticos
- ✅ Health checks

## Tecnologías

- **Spring Boot 3.5.4**
- **JPlag 6.2.0**
- **JGit** para manejo de repositorios Git
- **Java 21**

## Estructura del Proyecto

```
src/main/java/com/eci/iagen/jplag_service/
├── controller/
│   ├── PlagiarismController.java      # API REST principal
│   └── ReportController.java          # Servir reportes HTML
├── service/
│   ├── JPlagDetectionService.java     # Lógica principal JPlag
│   └── GitService.java                # Manejo de Git
├── dto/
│   ├── PlagiarismDetectionRequest.java
│   ├── PlagiarismDetectionResponse.java
│   └── SubmissionDto.java
├── config/
│   └── JPlagConfig.java               # Configuración
└── JplagServiceApplication.java
```

## API Endpoints

### Análisis de Plagio
```http
POST /api/plagiarism/analyze
Content-Type: application/json
```

**Request Body:**
```json
{
  "assignmentId": 123,
  "assignmentTitle": "Laboratorio de Programación",
  "submissions": [
    {
      "submissionId": 456,
      "teamId": 789,
      "teamName": "Equipo Alpha",
      "repositoryUrl": "https://github.com/usuario/repo1.git",
      "memberNames": ["Estudiante 1", "Estudiante 2"]
    },
    {
      "submissionId": 457,
      "teamId": 790,
      "teamName": "Equipo Beta",
      "repositoryUrl": "https://github.com/usuario/repo2.git",
      "memberNames": ["Estudiante 3", "Estudiante 4"]
    }
  ]
}
```

**Response:**
```json
{
  "assignmentId": 123,
  "assignmentTitle": "Laboratorio de Programación",
  "success": true,
  "message": "Análisis de plagio completado exitosamente",
  "reportUrl": "/reports/report_uuid/index.html",
  "comparisons": [
    {
      "submission1": "submission_456_team_789",
      "submission2": "submission_457_team_790",
      "team1": "Equipo Alpha",
      "team2": "Equipo Beta",
      "similarity": 0.85,
      "matchedTokens": 1250,
      "status": "completed"
    }
  ],
  "statistics": {
    "totalSubmissions": 2,
    "totalComparisons": 1,
    "averageSimilarity": 0.85,
    "maxSimilarity": 0.85,
    "minSimilarity": 0.85
  }
}
```

### Health Check
```http
GET /api/plagiarism/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "jplag-service",
  "version": "1.0.0"
}
```

### Información del Servicio
```http
GET /api/plagiarism/info
```

### Reportes HTML
```http
GET /reports/{reportId}/{file}
```

Sirve archivos estáticos del reporte HTML generado por JPlag.

## Configuración

### application.properties
```properties
# Server
server.port=8082

# JPlag Configuration
jplag.temp-directory=temp/jplag
jplag.reports-directory=reports
jplag.minimum-similarity=0.10

# Git Configuration
git.clone.timeout.seconds=60
git.max.concurrent.clones=5
```

## Proceso de Análisis

1. **Recepción**: Recibe solicitud del API Gateway con lista de entregas
2. **Validación**: Verifica que hay mínimo 2 entregas para comparar
3. **Clonado**: Clona repositorios Git de cada entrega
4. **Análisis**: Ejecuta JPlag para detectar similitudes
5. **Reporte**: Genera reporte HTML navegable
6. **Respuesta**: Retorna resultados y URL del reporte

## Requisitos

- Java 21+
- Git instalado en el sistema
- Acceso a internet para clonar repositorios
- 2GB RAM mínimo recomendado

## Instalación y Ejecución

1. **Clonar el proyecto**
```bash
git clone [repo-url]
cd jplag_service
```

2. **Compilar**
```bash
mvn clean compile
```

3. **Ejecutar**
```bash
mvn spring-boot:run
```

4. **Verificar**
```bash
curl http://localhost:8082/api/plagiarism/health
```

## Estructura de Directorios

```
jplag_service/
├── temp/           # Repositorios clonados temporalmente
├── reports/        # Reportes HTML generados
└── src/           # Código fuente
```

## Notas Importantes

- Los repositorios se clonan temporalmente y se procesan en memoria
- Los reportes HTML se generan en el directorio `reports/`
- Se requiere acceso de red para clonar repositorios Git
- JPlag analiza automáticamente archivos `.java` en los directorios `src/`

## Logging

El servicio registra todas las operaciones importantes:
- Clonado de repositorios
- Ejecución de JPlag
- Generación de reportes
- Errores y excepciones

## Integración con API Gateway

El servicio está diseñado para integrarse con el API Gateway que:
1. Obtiene entregas de la base de datos
2. Transforma datos al formato requerido
3. Envía solicitudes a este microservicio
4. Procesa y retorna resultados al frontend
