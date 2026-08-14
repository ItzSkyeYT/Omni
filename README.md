# Omni

> ### This is a fork. All credit for Omni belongs to its original authors.
>
> Omni was created by **[AkaneTan / 123Duo3](https://github.com/AkaneFoundation)** and the
> Akane Foundation contributors, and is maintained upstream at
> **[FoedusProgramme/Omni](https://github.com/FoedusProgramme/Omni)**.
>
> Everything that makes this app good — the Material 3 design, the hand-drawn compass and
> spirit level views, the physical-unit ruler, the variable-strength flashlight, and the
> decision to keep it small and free of bloat — is their work. This fork only adds an
> altitude readout and some landscape layouts on top of an app that was already excellent.
>
> If you are looking for Omni itself, get it from upstream:
> [GitHub releases](https://github.com/FoedusProgramme/Omni/releases/latest) ·
> [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/uk.akane.omni)

![GitHub](https://img.shields.io/github/license/AkaneFoundation/Omni?style=flat-square&logoColor=white&labelColor=black&color=white)
[![Static Badge](https://img.shields.io/badge/Telegram-Content?style=flat-square&logo=telegram&logoColor=black&color=white)](https://t.me/AkaneDev)

## Features

From the original app:

- Up-to-date Material 3 design
- Lightweight, no spyware or bloat
- Compass with latitude & longitude
- Spirit level
- Ruler
- Strength adjustable flashlight

Added in this fork:

- **Altitude on the compass**, corrected to height above sea level rather than the raw
  WGS84 ellipsoid height Android reports, which can differ by over 50 m
- **Metric / imperial** unit choice for the readout
- **Landscape layouts** for the compass and the ruler, where the ruler measures along the
  long edge and so roughly doubles its usable length
- **Battery level** on the flashlight screen
- **Swipe left and right** to move between tools
- Spirit level smoothing, and a readout that stays upright at any angle

## About this fork

Maintained by **[ItzSkyeYT](https://github.com/ItzSkyeYT)**. It tracks upstream and exists to
scratch a few personal itches, mainly wanting altitude on the compass while out walking.
Changes are listed above and each carries its reasoning in the commit message.

Modifications are marked as such in the app's About screen, as the GPL asks.

## Installation

This fork is not distributed anywhere. Build it yourself, or install the original from
[IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/uk.akane.omni).

## Building

You will need the latest [Android Studio](https://developer.android.com/studio) and a fast
network connection, then `./gradlew assembleDebug`.

The debug build carries an `applicationIdSuffix` of `.debug`, so it installs alongside a
release install of Omni rather than replacing it.

## License

GNU General Public License v3.0, unchanged from upstream — see [LICENSE](LICENSE).

Copyright for the original work remains with its authors. This fork is redistributed under
the same licence, as the GPL requires and as it deserves.

## Notice

- Bug reports for **Omni itself** belong upstream:
  [FoedusProgramme/Omni](https://github.com/FoedusProgramme/Omni/issues) or
  [Telegram](https://t.me/AkaneDev)
- Only report issues here if they are caused by the changes listed under *Added in this fork*
