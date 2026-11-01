# Kryn´s Waypoints

Rein clientseitige Fabric-Mod für Wegpunkte (Minecraft 26.3+).

- **B** (änderbar unter Steuerung): Wegpunkt an der aktuellen Position erstellen – Name, Koordinaten, 65 Symbole und 8x8-Farbwahl
- **Escape-Menü → „Optionen“**: Wechselentfernung zur XP-Leiste (50–2000), Markergröße, 50% Transparenz
- **Escape-Menü → „Waypointliste“** (oben rechts): Wegpunkte bearbeiten, ein-/ausblenden, löschen
- Bis zur eingestellten Entfernung (Standard 300): Symbol + Name + Entfernung in der Welt (durch Wände sichtbar), weiter weg: Symbol auf der XP-Leiste in Blickrichtung
- Beim Tod wird automatisch ein Wegpunkt am Todespunkt angelegt (abschaltbar in den Optionen)
- Gespeichert pro Welt/Server und Dimension in `config/kryns_waypoints.json`

## Bauen

```
./gradlew build
```

Die fertige Jar liegt in `build/libs/`. Alternativ baut GitHub Actions bei jedem Push und stellt die Jar als Artefakt bereit.

## Release

Jeder Push auf `main` baut die Mod und legt die Jar im Release `v<mod_version>` ab (Version in `gradle.properties`). Für ein neues Release `mod_version` erhöhen.

## Symbole

Die Symbole werden mit `tools/generate_icons.py` (Python + Pillow) erzeugt: `python3 tools/generate_icons.py src/main/resources/assets/kryns_waypoints` schreibt den Textur-Atlas und `icons.txt`, dazu eine Vorschau `sheet.png`.

## Wegpunkte teilen

In der Waypointliste gibt es pro Wegpunkt den Button **Teilen**. Er zeigt alle Spieler online, die die Mod nutzen. Ein Klick schickt ihnen den Wegpunkt, bei ihnen erscheint im Chat **[Annehmen]** / **[Ablehnen]**. Spieler mit der Mod bekommen im Tab-Menü ein gelbes ✦ hinter ihrem Namen.

Am Minecraft-Server ist dafür nichts nötig. Die Mods sprechen über einen eigenen kleinen Vermittlungsdienst miteinander:

1. Im Pelican-Panel einen Server mit einem Java-Egg (Java 21 oder neuer) anlegen und `kryns-waypoints-relay-<version>.jar` aus dem Release hochladen.
2. Startbefehl: `java -jar kryns-waypoints-relay-<version>.jar` – der Port wird automatisch aus `SERVER_PORT` (der Allocation) gelesen.
3. In Minecraft: Escape → **Optionen** → *Vermittlungsdienst* auf `IP:Port` des Pelican-Servers setzen. Alle, die teilen wollen, tragen dieselbe Adresse ein.

Anmeldung beim Dienst läuft über die Mojang-Sitzungsprüfung (wie beim Betreten eines Servers): Der Zugangstoken geht nur an Mojang, der Dienst erfährt nur den Spielernamen. Der Dienst speichert nichts dauerhaft. Teilen funktioniert nur mit Original-Accounts.
