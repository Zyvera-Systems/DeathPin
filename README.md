<div align="center">

<img src="https://cdn.modrinth.com/data/y1KgoaJb/ff39317a3179f267a18cf14f8cbbce805ba1ee45_96.webp" width="96" alt="DeathPin Icon"/>

# DeathPin

**Never lose your death spot again.**

After you die, DeathPin sends a clickable message in chat.
One click activates a particle trail that guides you straight back to your items.

[![Modrinth](https://img.shields.io/modrinth/v/deathpin?label=Modrinth&color=1bd96a)](https://modrinth.com/plugin/deathpin)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/deathpin?color=1bd96a)](https://modrinth.com/plugin/deathpin)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue)](LICENSE)

</div>

---

## How it works

1. You die
2. After respawn you see your death coordinates + direction in chat
3. Click **[✦ Partikelspur aktivieren]** — a glowing trail appears
4. Follow the particles back to your stuff
5. The trail stops automatically when you arrive

---

## Compatibility

| Platform | Version | Java |
|---|---|---|
| Paper / Spigot / Bukkit | 1.21.x | 21+ |
| Paper / Folia | 26.1+ | 25+ |
| Folia | 1.21.x | 21+ |

The JAR is compiled with Java 21 bytecode, which runs on both Java 21 (1.21.x) and Java 25 (26.1+) servers without changes.

---

## Commands

| Command | What it does |
|---|---|
| `/deathpin` | Toggle trail on/off |
| `/deathpin show` | Activate trail |
| `/deathpin hide` | Deactivate trail |
| `/deathpin info` | Show your last death location |
| `/deathpin reload` | Reload config *(op)* |

**Aliases:** `/dp` · `/pin` · `/dpin`

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `deathpin.use` | everyone | Use all basic commands |
| `deathpin.reload` | op | Reload the config |
| `deathpin.bypass.world` | op | Follow trail across worlds |

---

## Configuration

The full `config.yml` is generated on first startup. Key settings:

```yaml
message:
  show_coordinates: true   # show X/Y/Z in death message
  show_direction: true     # show compass direction

trail:
  particle: END_ROD        # particle type
  trail_length: 9.0        # how many blocks ahead the trail extends
  spacing: 0.5             # density of particles
  recalculate_ticks: 30    # path recalculation interval (30 = 1.5s)
  arrival_distance: 2.5    # auto-stop radius at death location
```

All messages support [MiniMessage](https://docs.advntr.dev/minimessage) formatting.

**Available particles:** `END_ROD` · `FLAME` · `SOUL` · `DUST` *(colored)* · `SPELL_WITCH` · `VILLAGER_HAPPY`



Requires Java 21 and Maven 3.8+.

---

<div align="center">

Made by **Zyvera-Systems** · **Thomas U.**

[Modrinth](https://modrinth.com/plugin/deathpin) · [Issues](https://github.com/Zyvera-Systems/DeathPin/issues) · [Discord](https://discord.gg/ZXebttwaJj)

</div>
