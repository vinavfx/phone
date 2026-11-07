# ph

CLI del teléfono. Usa el emparejamiento de `sms/` (`~/.config/sms/config.json`).

```bash
./install.sh
```

Instala el comando `ph` en `~/.local/bin`, disponible en zsh sin configuración adicional.

| Comando           | Resultado                                                       |
| ----------------- | --------------------------------------------------------------- |
| `ph`              | Estado de la conexión y lista de comandos; error si no responde |
| `ph code`         | Último código de verificación, con monto y RUT si corresponde   |
| `ph code -l`      | Último código de cada tipo                                      |
| `ph code -a`      | Todos los códigos recibidos                                     |
| `ph code SERVICE` | Filtra por servicio, por ejemplo `ph code whatsapp`             |

Reconoce Banco de Chile (pago, transferencia, registro en la app, autorización móvil), BancoEstado (pago, cambio de clave), SII, WhatsApp,
Entel, Uber, GoDaddy y Capital.com. Otros códigos se detectan de forma genérica con el remitente como servicio. Las reglas están en
`codes.py`.
