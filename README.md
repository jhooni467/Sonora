# Sonora

Sonora es un sistema de música con un control Android y un reproductor para televisión.

## Aplicaciones

- **Sonora Remote** (`:remote`): aplicación Android para leer los metadatos de la sesión multimedia activa del teléfono, controlar esa reproducción o controlar Sonora TV.
- **Sonora TV web**: reproductor completo mantenido en [jhooni467/Sonora-TV-](https://github.com/jhooni467/Sonora-TV-). También se ejecuta dentro del módulo Android TV.
- **Sonora TV Android** (`:tv`): aplicación para Android TV / Google TV que abre el reproductor web servido por Sonora Remote.

Los repositorios web y Android comparten el mismo archivo de reproductor. La copia que usa el servidor local del teléfono está en `remote/src/main/assets/sonora-tv/index.html`.

## Emparejar SONORA Android con SONORA TV

El televisor es quien inicia el emparejamiento y muestra el código:

1. Abre SONORA TV en el navegador del televisor.
2. Pulsa **CONECTAR SONORA**. La TV genera y muestra un código de seis cifras y un QR.
3. En Sonora Remote escribe ese código y pulsa **Vincular**. También puedes escanear el QR como alternativa.
4. Ambas aplicaciones consultan la misma configuración central `bridge-config.json` del repositorio SONORA TV. No tienes que escribir una dirección de servidor en el teléfono.
5. Cuando se confirme el canal WebSocket, ambas aplicaciones mostrarán que están conectadas.

El servidor SONORA Bridge debe estar desplegado por separado de GitHub Pages. Hasta que `bridgeUrl` en `bridge-config.json` tenga la dirección HTTPS real de ese servidor, el emparejamiento mostrará que el servicio todavía no está configurado. Consulta el archivo `PROTOCOL.md` del repositorio SONORA TV para el contrato de comunicación y el proceso de despliegue.

## Dos formas de usar la música

### Controlar la música que ya está sonando en el teléfono

Sonora Remote puede leer el título, artista y estado de la sesión multimedia activa de Android. Para ello, Android pide habilitar manualmente el acceso de notificaciones multimedia desde el botón **Abrir acceso multimedia**. Los controles de reproducción se envían al reproductor que mantiene esa sesión.

### Enviar un archivo del teléfono a Sonora TV

Con la TV conectada, pulsa **Enviar audio del teléfono al TV** y elige un archivo de audio. El teléfono lo sirve por la red local y Sonora TV lo reproduce. Mantén Sonora Remote activo mientras la transmisión está en uso; la dirección temporal del archivo depende del servidor del teléfono.

Con una TV emparejada puedes elegir el destino de los controles: **TELÉFONO** o **SONORA TV**.

## Funciones del reproductor TV

La experiencia web conserva su biblioteca IndexedDB, reproducción de archivos y enlaces directos de audio, portadas, favoritos, cola, aleatorio, repetición, volumen, barra de progreso, reproductor completo y edición de letras. También admite navegación con flechas y selección desde el mando cuando el navegador o WebView entrega esas teclas.

La página pública de GitHub Pages puede seguir funcionando como reproductor independiente. Para emparejarla con Sonora Remote, abre en la TV la dirección HTTP local servida por el teléfono. Los navegadores bloquean el intento de conectar desde una página HTTPS pública hacia un WebSocket local no cifrado.

## Privacidad y requisitos de red

- El teléfono y la TV deben estar en la misma red local y poder comunicarse entre sí.
- El emparejamiento usa un PIN de seis cifras generado al iniciar el servidor local.
- El canal local usa HTTP/WebSocket sin cifrado. Úsalo solo en una red privada de confianza; no expongas los puertos 8765 y 8766 a Internet.
- Si Sonora Remote se cierra o el teléfono deja la red, la TV perderá el enlace y la transmisión de archivos locales se interrumpirá.
- Los formatos de audio dependen de los códecs admitidos por el navegador o WebView de la TV.

## Compilar

Se requiere JDK 17 y Android SDK 37. El workflow **Android build** compila los módulos `:remote` y `:tv` y publica los APK Debug en el artefacto `sonora-debug-apks`.

## Estructura

```text
Sonora/
├── remote/    # Control Android, sesión multimedia y servidor local
├── tv/        # Aplicación Android TV basada en WebView
└── .github/workflows/android.yml
```
