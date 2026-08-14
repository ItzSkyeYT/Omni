# Omni

> ### This is a fork. All credit for Omni belongs to its original authors.
>
> Omni was created by **[AkaneTan / 123Duo3](https://github.com/AkaneFoundation)** and the
> Akane Foundation contributors, and is maintained upstream at
> **[FoedusProgramme/Omni](https://github.com/FoedusProgramme/Omni)**.
>
> Everything that makes this app good is their work: the Material 3 design, the hand-drawn
> compass and spirit level views, the physical-unit ruler, the variable-strength flashlight,
> and the decision to keep it small and free of bloat. This fork only adds an altitude
> readout and some landscape layouts on top of an app that was already excellent.
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

It ships under its own application id, `dev.skye.omni`, so it installs alongside a real Omni
rather than replacing or impersonating it, and its launcher icon is inverted so the two are
distinguishable at a glance. Modifications are marked as such in the app's About screen, as
the GPL asks.

## Installation

Grab the APK from [releases](https://github.com/ItzSkyeYT/Omni/releases/latest), or build it
yourself. Once installed it checks for its own updates under Settings > About.

Android will show a Play Protect "app scan recommended" prompt on install, because the app
does not come from a store. That is expected for any sideloaded app and cannot be suppressed
from inside it. [Obtainium](https://github.com/ImranR98/Obtainium) avoids the prompt and
tracks this repository's releases automatically.

## Building

You will need the latest [Android Studio](https://developer.android.com/studio) and a fast
network connection, then `./gradlew assembleDebug`.

Debug and release share one signing key, read from a gitignored `keystore.properties`, so a
release installs in place over a debug build. Android ties update identity to the signing
certificate, so a mismatch can only be resolved by uninstalling.

## License

GNU General Public License v3.0, unchanged from upstream. See [LICENSE](LICENSE).

Copyright for the original work remains with its authors. This fork is redistributed under
the same licence, as the GPL requires and as it deserves.

## Notice

- Bug reports for **Omni itself** belong upstream:
  [FoedusProgramme/Omni](https://github.com/FoedusProgramme/Omni/issues) or
  [Telegram](https://t.me/AkaneDev)
- Only report issues here if they are caused by the changes listed under *Added in this fork*
