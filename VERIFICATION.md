# Verificación de la entrega

Fecha: 1 de noviembre de 2026.

- `testDebugUnitTest assembleDebug`: BUILD SUCCESSFUL.
- 7 pruebas: 4 de catálogo y 3 de vistas/controles; 0 fallos y 0 errores.
- Se renderizaron las 52 variantes y 9 pantallas principales mediante Robolectric con gráficos nativos.
- Las capturas principales se inspeccionaron visualmente; se corrigieron el ajuste de las pantallas y las páginas del nano 6.
- `apksigner verify --verbose`: firma APK v2 válida, un firmante.
- Paquete: `com.vicente.podmuseum`, versión `1.0.0`, API mínima 26, objetivo 35.
- APK de desarrollo; no probado en un teléfono físico.
- OAuth y reproducción real de Spotify no se probaron con una cuenta del usuario: requieren su propio Client ID, permisos y un dispositivo Connect disponible.

SHA-256 del APK entregado:

```text
137ac2510677bbfed2d25620c4d1738e7dd04f756152fff401da30d0b22b3df3
```
