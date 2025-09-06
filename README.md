# Spring Config Server Demo with S3 Backend

A comprehensive demonstration of Spring Cloud Config Server using S3-compatible storage (DellEMC ECS simulation) with MinIO, featuring two client applications for different environments.

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Test Client   │    │   Config Server │    │   MinIO (S3)    │
│   Port: 8080    │◄──►│   Port: 8888    │◄──►│   Port: 9000    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                ▲
┌─────────────────┐            │
│   Dev Client    │            │
│   Port: 8090    │◄───────────┘
└─────────────────┘
```

## 🚀 Features

- **Spring Cloud Config Server** with S3 backend
- **MinIO** as S3-compatible storage (simulating DellEMC ECS)
- **Two client applications** (test and dev environments)
- **Beautiful web interfaces** for configuration display
- **REST APIs** for configuration access
- **Security** with basic authentication
- **Health checks** and monitoring endpoints
- **Easy startup/shutdown scripts**

## 📋 Prerequisites

- **Java 21** or higher
- **Maven 3.6+**
- **Docker & Docker Compose**
- **curl** (for health checks)

## 🔧 Quick Start

### 1. Clone and Navigate
```bash
cd /my-development-folder/.../spring-config
```

### 2. Start All Services
```bash
./start-demo.sh
```

This script will:
- Build all Maven projects
- Start MinIO S3 storage
- Start Config Server
- Start both client applications
- Verify all services are running

### 3. Access the Applications

| Service | URL | Credentials |
|---------|-----|-------------|
| **Test Environment** | http://localhost:8080 | None |
| **Dev Environment** | http://localhost:8090 | None |
| **Config Server** | http://localhost:8888 | config-user / config-pass |
| **MinIO Console** | http://localhost:9001 | minio-admin / minio-password |

### 4. Stop All Services
```bash
./stop-demo.sh
```

## 📱 Application Screenshots

### Test Environment (Blue Theme)
- **Service Name**: Demo Service - Test Environment
- **Parameter A**: test-value-alpha
- **Parameter B**: test-value-beta
- **Parameter C**: test-value-gamma
- **Features**: X=ON, Y=OFF, Z=ON

### Dev Environment (Orange Theme)
- **Service Name**: Demo Service - Development Environment
- **Parameter A**: dev-value-apple
- **Parameter B**: dev-value-banana
- **Parameter C**: dev-value-cherry
- **Features**: X=OFF, Y=ON, Z=OFF

## 🔗 API Endpoints

### Configuration APIs
- **Test Config JSON**: http://localhost:8080/api/config
- **Dev Config JSON**: http://localhost:8090/api/config

### Health Check APIs
- **Config Server**: http://localhost:8889/actuator/health
- **Test Client**: http://localhost:8080/actuator/health
- **Dev Client**: http://localhost:8090/actuator/health

### Config Server Direct Access
```bash
# Get test configuration (requires authentication)
curl -u config-user:config-pass http://localhost:8888/demo-service/test

# Get dev configuration (requires authentication)
curl -u config-user:config-pass http://localhost:8888/demo-service/dev
```

## 📁 Project Structure

```
spring-config-demo/
├── pom.xml                    # Parent Maven POM
├── docker-compose.yml         # MinIO S3 storage setup
├── start-demo.sh             # Startup script
├── stop-demo.sh              # Shutdown script
├── config-files/             # S3 configuration files
│   ├── application.yml       # Global configuration
│   ├── demo-service-test.yml # Test environment config
│   └── demo-service-dev.yml  # Dev environment config
├── config-server/            # Spring Config Server
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/demo/configserver/
│       │   ├── ConfigServerApplication.java
│       │   ├── SecurityConfig.java
│       │   ├── S3Config.java
│       │   └── S3ConfigEnvironmentRepository.java
│       └── resources/
│           └── application.yml
├── client-test/              # Test environment client
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/demo/client/test/
│       │   ├── TestClientApplication.java
│       │   ├── DemoProperties.java
│       │   └── ConfigController.java
│       └── resources/
│           ├── bootstrap.yml
│           ├── application.yml
│           └── templates/
│               └── config-display.html
└── client-dev/               # Dev environment client
    ├── pom.xml
    └── src/main/
        ├── java/com/demo/client/dev/
        │   ├── DevClientApplication.java
        │   ├── DemoProperties.java
        │   └── ConfigController.java
        └── resources/
            ├── bootstrap.yml
            ├── application.yml
            └── templates/
                └── config-display.html
```

## ⚙️ Configuration Details

### Spring Versions
- **Spring Boot**: 3.5.5
- **Spring Cloud**: 2025.0.0
- **Java**: 21 with preview features

### S3 Configuration
The Config Server connects to MinIO using:
- **Endpoint**: http://localhost:9000
- **Bucket**: config-bucket
- **Access Key**: minio-admin
- **Secret Key**: minio-password

### Security
- **Config Server**: Basic auth (config-user / config-pass)
- **Client Applications**: No authentication required
- **MinIO Console**: minio-admin / minio-password

## 🧪 Testing Configuration Changes

1. **Access MinIO Console**: http://localhost:9001
2. **Login** with minio-admin / minio-password
3. **Navigate** to config-bucket
4. **Edit** configuration files (e.g., demo-service-test.yml)
5. **Refresh** client application configuration:
   ```bash
   curl -X POST http://localhost:8080/actuator/refresh
   curl -X POST http://localhost:8090/actuator/refresh
   ```
6. **View updated values** in the web interface

## 🔧 Manual Setup (Alternative)

If you prefer to start services individually:

### 1. Start MinIO
```bash
docker-compose up -d
```

### 2. Build Projects
```bash
mvn clean install
```

### 3. Start Config Server
```bash
cd config-server
mvn spring-boot:run
```

### 4. Start Test Client
```bash
cd client-test
mvn spring-boot:run
```

### 5. Start Dev Client
```bash
cd client-dev
mvn spring-boot:run
```

## 🐛 Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 8080, 8081, 8888, 8090, 8091, 9000, 9001 are available
2. **MinIO not ready**: Wait for MinIO health check to pass
3. **Config Server connection**: Verify S3 credentials and bucket access
4. **Client startup**: Ensure Config Server is running before starting clients

### Logs
Check individual service logs in the `logs/` directory:
- `config-server.log`
- `client-test.log`
- `client-dev.log`

### Health Checks
```bash
# Check all services
curl http://localhost:8889/actuator/health  # Config Server
curl http://localhost:8081/actuator/health  # Test Client
curl http://localhost:8091/actuator/health  # Dev Client
curl http://localhost:9000/minio/health/live # MinIO
```

## 🎯 Key Learning Points

1. **S3 Backend Configuration**: How to configure Spring Config Server with S3-compatible storage
2. **Environment Separation**: Managing different configurations for test/dev environments
3. **Security Integration**: Implementing authentication for configuration access
4. **Client Configuration**: Using bootstrap.yml for Config Server client setup
5. **Property Binding**: Using @ConfigurationProperties with Java records
6. **Web Interface**: Creating responsive configuration display pages
7. **Monitoring**: Setting up health checks and actuator endpoints

## ☁️ Cloud Foundry Deployment

This application now supports deployment to Cloud Foundry with VCAP_SERVICES integration for S3 configuration.

### Prerequisites for Cloud Foundry
- **cf CLI** installed and logged in to your CF instance
- **S3 service** available in your CF marketplace
- **Java buildpack** available

### VCAP Services Configuration

The application automatically detects and uses VCAP_SERVICES when available. It supports multiple S3 service types:

#### 1. Create S3 Service Instance

```bash
# For DellEMC ECS
cf create-service dell-ecs s3 config-s3-service -c '{
  "endpoint": "https://your-ecs-endpoint.com",
  "access-key": "your-access-key", 
  "secret-key": "your-secret-key",
  "bucket": "config-bucket",
  "region": "us-east-1"
}'

# For Generic S3 (AWS S3 or S3-compatible)
cf create-service s3 standard config-s3-service -c '{
  "endpoint": "https://s3.amazonaws.com",
  "access-key": "your-access-key",
  "secret-key": "your-secret-key", 
  "bucket": "config-bucket",
  "region": "us-east-1"
}'

# For User-Provided Service (custom S3 endpoint)
cf create-user-provided-service config-s3-service -p '{
  "endpoint": "https://your-custom-s3.com",
  "access-key": "your-access-key",
  "secret-key": "your-secret-key",
  "bucket": "config-bucket",
  "region": "us-east-1"
}'
```

#### 2. Deploy Application

```bash
# Build the application
mvn clean package -DskipTests

# Deploy to Cloud Foundry
cf push -f manifest.yml

# Bind service if not already bound
cf bind-service spring-config-server config-s3-service
cf restage spring-config-server
```

### VCAP Services JSON Format

The application supports flexible field names for S3 credentials in VCAP_SERVICES:

```json
{
  "s3": [
    {
      "name": "config-s3-service",
      "label": "s3",
      "plan": "standard",
      "credentials": {
        "endpoint": "https://your-s3-endpoint.com",
        "access-key": "your-access-key",
        "secret-key": "your-secret-key",
        "bucket": "config-bucket",
        "region": "us-east-1"
      }
    }
  ]
}
```

### Supported Field Name Variations

The VCAP configuration parser recognizes multiple field name formats:

- **Endpoint**: `endpoint`, `uri`, `url`, `host`
- **Access Key**: `access-key`, `accessKey`, `access_key`, `username`
- **Secret Key**: `secret-key`, `secretKey`, `secret_key`, `password`
- **Bucket**: `bucket`, `bucket-name`, `bucketName`, `bucket_name`
- **Region**: `region`, `aws-region`, `awsRegion`

### Service Name Detection

The application searches for S3 services under these names in VCAP_SERVICES:
- `s3` (default)
- `dell-ecs`
- `ecs-s3`
- `object-storage`
- `aws-s3`

You can specify a custom service name using the `S3_SERVICE_NAME` environment variable.

### Cloud Foundry Environment Variables

For Cloud Foundry deployment, set these environment variables as needed:

```bash
cf set-env spring-config-server SPRING_PROFILES_ACTIVE cloud
cf set-env spring-config-server S3_SERVICE_NAME your-service-name
cf set-env spring-config-server SECURITY_USER_PASSWORD your-secure-password
```

### Local VCAP Testing

The `start-demo.sh` script now sets a local VCAP_SERVICES environment variable for testing:

```bash
export VCAP_SERVICES='{
  "s3": [
    {
      "name": "config-s3-service",
      "credentials": {
        "endpoint": "http://localhost:9000",
        "access-key": "minio-admin",
        "secret-key": "minio-password",
        "bucket": "config-bucket",
        "region": "us-east-1"
      }
    }
  ]
}'
```

### Configuration Priorities

The application uses the following configuration priority order:
1. **VCAP_SERVICES** (when available in Cloud Foundry)
2. **Environment variables** (S3_ENDPOINT, S3_ACCESS_KEY, etc.)
3. **Application properties** (application.yml fallback values)

## 🚀 Next Steps

To extend this demo:
1. **Add encryption** for sensitive configuration values
2. **Implement refresh scope** for dynamic configuration updates
3. **Add more environments** (staging, production)
4. **Integrate with Git** as an additional configuration source
5. **Add configuration validation** and schema enforcement
6. **Implement configuration auditing** and change tracking
7. **Deploy to Cloud Foundry** using VCAP services integration

## 📝 Notes

- This demo uses MinIO to simulate DellEMC ECS Storage behavior
- All configurations are stored in YAML format for readability
- The S3ConfigEnvironmentRepository provides custom S3 integration
- Bootstrap context is used for Config Server client configuration
- Health checks ensure proper service startup order