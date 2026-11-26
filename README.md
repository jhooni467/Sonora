# Sonora

SONORA combina un control musical Android con SONORA TV, un reproductor web para televisores.

## Repositorios

- **SONORA Android:** https://github.com/jhooni467/Sonora
- **SONORA TV Web:** https://github.com/jhooni467/Sonora-TV-
- **SONORA TV publicada:** https://jhooni467.github.io/Sonora-TV-/

## Emparejamiento Android ↔ TV

**La televisión genera el código; SONORA Android lo introduce.**

1. Abre SONORA TV desde el navegador del televisor.
2. En la TV selecciona **CONECTAR SONORA**. La pantalla de conexión genera un código de seis cifras y un QR.
3. En Sonora Remote escribe el código mostrado en el televisor y pulsa **Vincular**. El escaneo QR queda disponible como alternativa.
4. Android lee automáticamente el servidor configurado en el archivo público `bridge-config.json` del repositorio SONORA TV. No se solicita al usuario que escriba una dirección de servidor.
5. El teléfono y la TV se autentican con el Bridge y abren un canal WebSocket para los controles y el estado de conexión.

La dirección compartida se obtiene de:

`https://jhooni467.github.io/Sonora-TV-/bridge-config.json`

El campo `bridgeUrl` debe contener la dirección HTTPS del Bridge desplegado. Si está vacío, Sonora Remote muestra que el servidor todavía no está configurado.

## Funciones de Sonora Remote

- Introducir el código de seis cifras generado por la TV.
- Escanear el QR de SONORA TV.
- Detectar si la TV está realmente conectada.
- Reconectar el WebSocket si hay una interrupción de red.
- Seleccionar una canción desde el teléfono.
- Leer título, artista, álbum, duración y carátula incrustada.
- Enviar un archivo completo o transmitir audio mediante SONORA Bridge.
- Enviar letras manualmente.
- Enviar controles de anterior, reproducir/pausar y siguiente.

La conexión usa HTTPS/WSS a través de SONORA Bridge. El teléfono y la TV no necesitan estar en la misma red local, siempre que ambos tengan acceso a Internet.

## SONORA Bridge

El servidor Node.js está en el repositorio SONORA TV, dentro de `server/`. GitHub Pages no ejecuta el servidor: debe desplegarse como un servicio independiente (el repositorio incluye `render.yaml`).

Pasos para activarlo:

1. Despliega el Blueprint de SONORA TV en Render.
2. Copia la dirección HTTPS que Render asigne al servicio.
3. Actualiza `bridgeUrl` en el archivo `bridge-config.json` de SONORA TV.
4. Espera a que GitHub Pages publique esa configuración.
5. Abre SONORA TV y SONORA Remote; ambos leerán automáticamente la misma dirección.

Hasta completar estos pasos, el flujo de interfaz está implementado pero el emparejamiento real no puede establecerse porque todavía no hay servidor desplegado.

## Compilación Android

Se requiere JDK 17 y Android SDK 37. El workflow **Android build** compila los módulos `:remote` y `:tv` y publica los APK Debug como artefacto de GitHub Actions.

## Estructura

```text
Sonora/
├── remote/    # Control Android y emparejamiento con SONORA TV
├── tv/        # Aplicación Android TV basada en WebView
└── .github/workflows/android.yml
```
