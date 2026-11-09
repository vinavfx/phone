# Prefix Blocker

Aplicación Android mínima para rechazar **solo llamadas entrantes** cuyos números comiencen con un prefijo de la lista. Las llamadas
salientes nunca se bloquean, aunque coincidan con un prefijo.

## Uso

1. Abre **Prefix Blocker** y activa el bloqueo:
   - **Android 9:** pulsa **Set as default phone app** y acepta sustituir al marcador predeterminado. Es necesario para filtrar antes del
     timbre.
   - **Android 10 o posterior:** pulsa **Enable call blocking** para seleccionarla como filtro, sin cambiar tu aplicación de teléfono.
2. Pulsa **Include saved contacts** y concede el permiso si también quieres bloquear números guardados. Sin él, Android excluye los
   contactos del filtrado. La aplicación no consulta ni transmite tu agenda.
3. Pulsa **Hide blocked calls from history** para que la aplicación borre del historial las llamadas bloqueadas que coincidan con tus
   prefijos. `install.sh` concede este permiso automáticamente.
4. Añade prefijos con **Add** y elimínalos con **Delete**. La lista se guarda en el teléfono y comienza vacía en instalaciones nuevas.

`600` bloquea los números nacionales que empiezan por `600`, incluso si llegan como `+56 600…`. `80` funciona del mismo modo. Un prefijo con
`+`, como `+56600`, restringe la coincidencia al código internacional indicado. La normalización utiliza libphonenumber y el país de la red
o de la SIM.

Funciona sin conexión, publicidad, cuenta ni permiso de Internet. No es necesario mantener abierta la pantalla: Android inicia el servicio
al recibir una llamada. No fuerces la detención de la aplicación desde Ajustes.

### Modo de teléfono en Android 9

El botón **Open dial pad** abre el marcador. Las llamadas permitidas tienen controles para atender con audio, rechazar, colgar, silenciar,
cambiar la salida de audio, poner en espera y marcar tonos. La selección de SIM, las conferencias y añadir llamadas dependen del operador.
Android conserva la gestión del timbre y de las llamadas de emergencia; el filtro no restringe las llamadas salientes.

Solo las llamadas permitidas muestran una pantalla y una notificación de control; las bloqueadas se rechazan desde el servicio de filtrado.
El marcador es básico: no incluye historial, agenda ni interfaz de videollamada. Para recuperar el marcador anterior, cambia la aplicación
predeterminada de teléfono en Ajustes. En Android 9 eso también desactiva el filtrado de prefijos. En Android 10 o posterior los componentes
del marcador están deshabilitados y no sustituyen a tu aplicación de teléfono.

## Alcance y límites

- Android 9 o posterior. Android 9 exige ser el teléfono predeterminado; las versiones posteriores utilizan el rol de filtrado de llamadas.
  Solo una aplicación puede ocupar cada función a la vez.
- Se utiliza `CallScreeningService` para rechazar la llamada antes del timbre y solicitar que no haya notificación de llamada perdida.
- Android registra igualmente la llamada como bloqueada; con el permiso del historial, la aplicación la borra unos segundos después y al
  abrirse.
- Los números ocultos o no identificados no tienen un prefijo que comparar. No se filtran llamadas de WhatsApp ni de otras aplicaciones.
- Las excepciones del sistema, por ejemplo tras una llamada de emergencia, y el desvío al buzón de voz dependen de Android y del operador.

## Instalar en otros teléfonos por USB

La APK reutilizable queda en `dist/prefix-blocker.apk`. Para instalarla desde este ordenador solo necesitas ADB, que ya está instalado; no
hace falta recompilar ni descargar herramientas de desarrollo.

1. En el teléfono, activa las opciones de desarrollador pulsando siete veces **Número de compilación** en los ajustes de información del
   dispositivo. La ubicación varía según la marca.
2. Activa **Depuración USB**, conecta un cable de datos y acepta **Permitir depuración USB** en el teléfono desbloqueado.
3. Ejecuta:

```bash
cd /home/pancho/Documents/develop/phone/prefix-blocker
./install.sh
```

Si hay varios dispositivos, consulta `adb devices -l` y usa `./install.sh SERIAL` con el identificador del teléfono deseado. El instalador
comprueba la conexión y Android 9 o posterior, instala o actualiza sin borrar la lista y abre la aplicación. Después, activa el filtro y
añade los prefijos siguiendo la sección **Uso**. Las instalaciones nuevas comienzan sin prefijos.

Para pedírmelo después: **«Instala Prefix Blocker del repositorio /home/pancho/Documents/develop/phone/prefix-blocker en el teléfono
conectado por USB y ayúdame a activar el filtro»**.

Para compartirla, basta copiar la APK; su instalación directa requiere permitir aplicaciones de esa fuente. El SDK y `.tools/` no son
necesarios para instalarla. Los artefactos de `dist/` se conservan localmente, pero no están versionados en Git.

## Compilar una versión actualizada

Requisitos: Java 17, Gradle 8.7 y SDK Android con `platforms;android-34` y `build-tools;34.0.0`. Configura `JAVA_HOME` y `ANDROID_HOME`.
`build.sh` también reconoce las herramientas locales de `../.tools/`, excluidas de Git.

```bash
./build.sh
sha256sum -c dist/prefix-blocker.apk.sha256
./install.sh
```

La compilación incluye el análisis estático de Android y actualiza la copia de `dist/` y su suma SHA-256. Es una APK de desarrollo firmada,
válida para instalación manual, no una publicación de tienda. Conserva `~/.android/debug.keystore`: las futuras actualizaciones deben usar
la misma clave para mantener la compatibilidad con las instalaciones existentes. No compartas ni publiques esa clave.

## Verificación en el dispositivo

La versión inicial se instaló mediante USB en un Motorola moto e14 con Android 14; se verificaron el rol, el permiso para contactos y la
gestión de prefijos. La versión 1.1 se instaló en un moto g6 plus con Android 9 y se seleccionó como teléfono predeterminado.

Las comprobaciones temporales con Android 9 y Android 14 simulados verificaron la activación, la gestión de prefijos, las coincidencias
nacionales e internacionales, las respuestas de rechazo sin notificación, las salientes permitidas y los controles básicos de llamadas.
Falta verificar con llamadas reales el rechazo silencioso y el funcionamiento del marcador en el teléfono.

Referencia: [servicio nativo de filtrado de Android](https://developer.android.com/reference/android/telecom/CallScreeningService).
