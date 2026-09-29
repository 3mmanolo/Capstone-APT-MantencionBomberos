# Imagen base con OpenJDK 17 en Ubuntu
FROM eclipse-temurin:17-jdk-jammy

# Variables de entorno para Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0

# Instalar dependencias necesarias del sistema (curl, unzip, git, etc.)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    unzip \
    git \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Descargar e instalar Android Command-Line Tools
RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    curl -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Aceptar licencias del SDK de Android e instalar Build Tools y Platforms
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Crear directorio de trabajo para la app Android
WORKDIR /workspace

# Copiar archivos de configuración de Gradle para aprovechar la caché de Docker
COPY gradle /workspace/gradle
COPY gradlew /workspace/gradlew
COPY gradlew.bat /workspace/gradlew.bat
COPY build.gradle.kts /workspace/build.gradle.kts
COPY settings.gradle.kts /workspace/settings.gradle.kts
COPY gradle.properties /workspace/gradle.properties

# Dar permisos de ejecución al script Wrapper de Gradle
RUN chmod +x gradlew

# Copiar el código fuente completo del módulo app
COPY app /workspace/app

# Comando por defecto: Construir la APK de depuración (Debug APK)
CMD ["./gradlew", "assembleDebug", "--stacktrace"]
