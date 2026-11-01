# Pod Museum · Android

Aplicación Android nativa en Java. Recrea las cinco familias de iPod con 52 entradas de catálogo: generaciones, revisiones principales y ediciones especiales seleccionadas.

No utiliza WebView ni reutiliza el HTML anterior. Las carcasas y controles se dibujan con Canvas; la pantalla es un contenedor Android medido según las proporciones de cada modelo. Los touch tienen una pantalla completa con iconos; los shuffle no tienen pantalla y utilizan un panel auxiliar separado.

## Instalar

Descarga `PodMuseum-debug.apk` del paquete de entrega, o el artefacto `PodMuseum-debug-apk` en GitHub → Actions → Android APK. El APK es de depuración, firmado con una clave de desarrollo; no es una publicación en Google Play. Si una compilación posterior usa otra clave, Android exige desinstalar la anterior antes de instalarla y se pierden los ajustes locales.

Android 8.0 / API 26 o posterior. No necesita permisos de almacenamiento, micrófono o ubicación.

## Spotify

Esta aplicación es un **controlador de Spotify Connect**. La música se reproduce en la aplicación oficial Spotify del teléfono o en otro dispositivo Connect; no incluye un motor de streaming independiente ni descarga canciones. El SDK web de Spotify no se utiliza dentro de Android.

1. Crea una aplicación en https://developer.spotify.com/dashboard y habilita Web API.
2. Registra exactamente esta Redirect URI nativa: `com.vicente.podmuseum://callback`.
3. Si el panel solicita los datos Android: package `com.vicente.podmuseum`. La huella SHA-1 depende de tu clave de firma; consulta `./gradlew signingReport`. El login PKCE de este proyecto abre el navegador, no el SDK App Remote.
4. En Development Mode autoriza tu usuario en el panel cuando sea necesario. Los requisitos de Premium y límites los determina Spotify.
5. Instala Spotify, inicia sesión y reproduce una canción para que aparezca como dispositivo disponible.
6. Abre Pod Museum → Ajustes → pega el **Client ID** → Guardar y conectar.
7. Autoriza en el navegador. Android volverá a la app por el enlace nativo.
8. En Ajustes → Seleccionar dispositivo, elige el teléfono o un dispositivo Connect disponible.

No introduzcas un Client Secret: una aplicación móvil no puede mantenerlo confidencial.

Permisos OAuth: perfil privado, estado y control de reproducción, playlists privadas y colaborativas. Se utiliza Authorization Code con PKCE S256, `state` aleatorio y vencimiento del intento. El refresh token y el verificador se guardan cifrados con AES-GCM y una clave de Android Keystore. Los backups están deshabilitados.

Los tokens no se incluyen en el repositorio ni se envían a un servidor propio. Se usan HTTPS y los endpoints oficiales de Spotify. El polling se detiene cuando la Activity pasa a segundo plano y respeta los tiempos `Retry-After` de Spotify.

### Límites de integración

- Spotify Premium es necesario para los controles Web API correspondientes.
- Un dispositivo tiene que estar disponible y permitir control remoto; las restricciones regionales y del contenido siguen vigentes.
- La consulta de canciones de ciertas playlists ajenas está restringida en Development Mode. Se conserva la opción de reproducir la playlist completa cuando Spotify lo permita.
- La búsqueda pagina 10 resultados por solicitud. Los endpoints de playlists usan `/items`, con compatibilidad para aplicaciones antiguas mediante `/tracks` cuando el primero devuelve 404.
- El volumen remoto depende de `supports_volume`; el volumen de Spotify en el teléfono también puede ajustarse con los botones físicos de Android.
- El login y reproducción real requieren un Client ID y cuenta autorizada propios; no se simula una conexión exitosa.

## Controles

- Rueda: arrastrar circularmente para navegar; centro para seleccionar.
- MENU: volver; los botones laterales controlan anterior y siguiente; abajo reproduce/pausa.
- Tercera generación: conserva sus cuatro botones independientes sobre la rueda.
- Touch / nano táctil: toca iconos y listas; el botón físico de inicio vuelve al menú.
- Shuffle: botones reales en los modelos que los tenían. En el tercero, el control remoto auxiliar representa los controles que originalmente iban en los auriculares.
- El control inferior de la app proporciona botones Android accesibles para todas las carcasas.
- Toca el nombre del modelo para cambiar color, ver información o girar al reverso.
- Las ediciones grabadas usan texto representativo; no se afirma que los grabados o firmas sean facsímiles exactos.

## Compilar

Requisitos: Android Studio compatible con AGP 8.7.3, JDK 17, SDK Android 35 y Gradle 8.9. Abre la carpeta raíz en Android Studio y sincroniza.

```sh
./gradlew testDebugUnitTest assembleDebug
```

Salida: `app/build/outputs/apk/debug/app-debug.apk`.

```sh
./gradlew signingReport
```

Para una entrega pública, configura tu propia clave y firma una build release. No subas keystores, `local.properties`, tokens ni credenciales al repositorio.

## GitHub

La carpeta está preparada para un repositorio nuevo. Para publicarla desde tu ordenador:

```sh
git init -b main
git add .
git commit -m "Build native Android iPod museum and Spotify controller"
```

Después añade la URL de **tu repositorio vacío** mediante `git remote add origin URL` y ejecuta `git push -u origin main`. No uses un repositorio ajeno o que contenga otro proyecto.

El workflow `.github/workflows/android.yml` ejecuta pruebas y compila un APK de depuración en cada push a `main`, PR o ejecución manual. No publica nada en Google Play.

## Estructura

- `Model.java`: catálogo, materiales y geometría relativa.
- `DeviceView.java`: carcasa nativa, pantalla medida y gestos de rueda.
- `MainActivity.java`: navegación, iconos táctiles, biblioteca y controles.
- `SpotifyClient.java`: OAuth PKCE, renovación de tokens y API.
- `TokenVault.java`: persistencia cifrada con Android Keystore.
- `ModelTest.java`: integridad del catálogo y pantallas dentro de cada carcasa.
- `DeviceLayoutTest.java`: medidas, controles y capturas con gráficos nativos mediante Robolectric.

## Verificación

Las pruebas de gráficos nativos generan capturas en `app/build/reports/screenshots`. Las capturas proceden de vistas Android renderizadas por Robolectric; no sustituyen la prueba en un teléfono físico. La integración real con Spotify se debe verificar con el Client ID y la cuenta de su propietario.

## Fuentes

- Identificación de iPods: https://support.apple.com/es-cl/103823
- OAuth PKCE: https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow
- Autorización Android y esquemas nativos: https://developer.spotify.com/documentation/android/tutorials/authorization
- Spotify Web API: https://developer.spotify.com/documentation/web-api
- Cambios Development Mode: https://developer.spotify.com/documentation/web-api/tutorials/february-2026-migration-guide

Proyecto independiente sin afiliación con Apple ni Spotify. Las marcas, carátulas y grabaciones pertenecen a sus respectivos titulares.
