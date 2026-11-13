# Donut Pay Alerts (Fabric, Minecraft 26.2, client-side)

Plays an on-screen animation when someone pays you. Nothing is installed on the server.

| Payment | Animation |
|---|---|
| $100K to under $500K | Popup on the right side with player head, name and amount |
| $500K to under $1M | Fireworks in the middle of the screen |
| $1M to under $10M | Gold coins falling through the middle of the screen |
| $10M to under $50M | Nuke: incoming missile, flash, shaking mushroom cloud |
| $50M and up | Universal: rotating spiral galaxy |

Small popups stack. The bigger animations play one at a time, in order.

## Build

You need JDK 25 and Gradle 9.5+.

Easiest: generate a fresh project at https://fabricmc.net/develop/ (Minecraft 26.2, mod id `donutpay`, package `com.donutpay`), then copy this project's `src/` folder over the generated one. Its build files will have the newest loader and Fabric API versions.

Or use this folder as is: run `gradle wrapper`, then `./gradlew build`. The jar is in `build/libs/`. Put it in your `mods` folder with Fabric API.

## First run: match the real payment message

The default pattern is a guess: `Steve paid you $250K`. To see what DonutSMP really sends:

1. Open `config/donutpay.json` and set `"logSystemMessages": true`.
2. Run `/donutpay reload`, get paid once, then find the line in `logs/latest.log` (it starts with `[system message]`).
3. Change `messagePattern` to match. It needs two named groups, `(?<player>...)` and `(?<amount>...)`. Run `/donutpay reload`.

## Commands

- `/donutpay test <player> <amount>`: preview an animation, e.g. `/donutpay test Steve 5M`
- `/donutpay reload`: reload `config/donutpay.json`

## Config

Thresholds (`small`, `medium`, `large`, `huge`, `universal`), `sounds`, `enabled` and `messagePattern` are all in `config/donutpay.json`.

## Notes

- Heads come from the tab list. If a payer isn't listed, a default skin is shown.
- Only server system messages are read, not player chat, so other players can't fake a payment by typing one.
- Check DonutSMP's rules on client mods before using this on the server.
