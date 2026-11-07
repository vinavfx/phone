# SMS

Visor de solo lectura de los SMS del teléfono en el escritorio Linux, a través de la red local. No permite enviar mensajes.

- `android/`: aplicación **SMS Sync** (Android 10 o posterior). Un servicio en primer plano comparte los SMS en el puerto `8765`, protegido
  con un token aleatorio. Se inicia también al arrancar el teléfono.
- `linux/`: aplicación **SMS** (Python + Tkinter). Muestra las conversaciones, consulta cada 3 segundos y notifica los mensajes nuevos.

## Instalar y emparejar

Con el teléfono conectado por USB, con depuración USB y en la misma Wi-Fi que el ordenador:

```bash
./install.sh
```

Instala la APK, concede los permisos, la excluye del ahorro de batería, copia el token y la IP a `~/.config/sms/config.json` e instala la
aplicación de escritorio en `~/.local/share/sms`, con el comando `sms` y un lanzador en el menú. Después, el cable ya no es necesario. Si
cambia la IP del teléfono, la aplicación de escritorio lo busca en la red `/24` local.

## Compilar

```bash
android/build.sh
```

Requiere las herramientas de `../.tools/` (ver el README de `prefix-blocker`). Genera `android/dist/sms-sync.apk`.
