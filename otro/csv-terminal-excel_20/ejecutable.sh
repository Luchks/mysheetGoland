#!/bin/bash
# ==============================================
# Instalador del programa CSV Terminal Excel
# para abrir archivos .csv en todo el sistema
# Autor: luchks
# Sistema: Arch Linux
# ==============================================

set -e

# 🔧 CONFIGURACIÓN
APP_NAME="csvexcel"
MAIN_CLASS="com.example.App"
PROJECT_DIR="$(pwd)"   # Directorio actual del proyecto
JAR_NAME="csv-terminal-excel-1.0-SNAPSHOT.jar"
TARGET_JAR="$PROJECT_DIR/target/$JAR_NAME"
LAUNCHER_PATH="/usr/local/bin/$APP_NAME"
DESKTOP_FILE="$HOME/.local/share/applications/${APP_NAME}.desktop"

echo "🚀 Iniciando instalación de $APP_NAME..."

# 1️⃣ Empaquetar el proyecto con Maven
echo "🔧 Empaquetando proyecto Maven..."
mvn clean package -q

if [ ! -f "$TARGET_JAR" ]; then
    echo "❌ Error: no se encontró el archivo $TARGET_JAR"
    exit 1
fi
echo "✅ JAR generado correctamente."

# 2️⃣ Crear script ejecutable global
echo "📦 Creando lanzador global en $LAUNCHER_PATH..."

sudo bash -c "cat > $LAUNCHER_PATH" <<EOF
#!/bin/bash
java -jar "$TARGET_JAR" "\$@"
EOF

sudo chmod +x "$LAUNCHER_PATH"
echo "✅ Script ejecutable creado."

# 3️⃣ Crear archivo .desktop
echo "🖥️  Creando archivo .desktop en $DESKTOP_FILE..."

mkdir -p "$(dirname "$DESKTOP_FILE")"

cat > "$DESKTOP_FILE" <<EOF
[Desktop Entry]
Name=CSV Terminal Excel
Comment=Abrir archivos CSV con tu programa Java en terminal
Exec=$LAUNCHER_PATH %f
Icon=accessories-text-editor
Terminal=true
Type=Application
MimeType=text/csv;
Categories=Utility;
EOF

# 4️⃣ Registrar MIME y asociación
echo "🔗 Actualizando base de datos de MIME..."
update-desktop-database ~/.local/share/applications

echo "🔗 Asociando archivos .csv con $APP_NAME..."
xdg-mime default "$(basename "$DESKTOP_FILE")" text/csv

# 5️⃣ Confirmación
echo "✅ Instalación completada con éxito."
echo "Prueba abriendo un archivo CSV, o ejecuta:"
echo "    $APP_NAME /ruta/a/archivo.csv"
