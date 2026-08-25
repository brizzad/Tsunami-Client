# Module inventory

Upstream ships **238 modules**. This is every one of them, sorted
into what Tsunami keeps, what needs a decision, and what goes.

Generated from the module tree joined against
`src/main/resources/resources/liquidbounce/lang/en_us.json`, so the
descriptions are upstream's own words rather than my reading of the class
names. Regenerate with `scripts/inventory.mjs` after any upstream merge.

**This is a proposal, not a done deal.** Scope is Nathan's call — the REVIEW
section exists because those are genuinely arguable, and anything in KEEP can
be argued back out.

| Verdict | Modules | Share | Lines |
| --- | ---: | ---: | ---: |
| Keep | 33 | 14% | 4,696 |
| Review | 12 | 5% | 1,666 |
| Remove | 193 | 81% | 30,763 |

Roughly 81% of the module surface goes. That is the fork in
one number: Tsunami is not LiquidBounce with a new coat of paint, it is the
framework plus a different 45 modules.

---

## Keep (33)

Each of these maps onto something the brief actually asks for, or is framework
the client cannot run without.

| Module | Category | Why | Upstream description |
| --- | --- | --- | --- |
| `AntiBlind` | render | brief: fire overlay reduction — visibility while burning (toggle) | Protects you from potentially annoying screen effects that obscure your view. |
| `BetterChat` | misc | brief: chat customization | Improvements to the in-game chat. |
| `BetterTab` | misc | brief: boss bar/scoreboard/tab list customization | Multiple improvements to the tab list. |
| `BetterTitle` | misc | brief: titles customization | Improvements to the title and subtitle. |
| `BlockOutline` | render | brief: block outline/overlay | Changes the way Minecraft highlights blocks. |
| `ClickGui` | render | The module/settings GUI. Core framework — the reason LiquidBounce was chosen as the base. | — |
| `Crosshair` | render | brief: crosshair customization | Changes the style of your crosshair. |
| `CustomAmbience` | render | brief: weather/time changer | Allows you to override the world ambience. |
| `Debug` | render | Developer tooling. Harmless and useful while building the fork. | Only of interest to developers. |
| `FreeLook` | render | brief: freelook/perspective | Allows you to move the camera freely around your character. |
| `FullBright` | render | brief: fullbright/gamma | Makes the world a brighter place. |
| `Hats` | render | brief: cosmetics system (hats) | Render a hat above the player's head. |
| `HitFX` | render | brief: hit color | Hitting a target triggers a special effect. |
| `Hud` | render | The in-game HUD framework. Everything in the HUD & Visual scope hangs off this. | Shows an in-game overlay with various useful tools. |
| `InventoryTracker` | misc | brief: item physics/counter/tracker | Tracks the inventories of other players. |
| `ItemTags` | render | brief: item counter/tracker | Display icons and quantities labels for dropped items. |
| `JumpEffect` | render | Cosmetic only. | Shows an effect beneath your feet when jumping. |
| `Macros` | misc | brief: macros/keybinds | Lets you execute chat messages or item actions using custom keybinds. |
| `NameProtect` | misc | brief: nick hider | Hides the player's real username client-side (useful for videos). |
| `Nametags` | render | brief: titles/name tag customization | Improves the visibility of player's name tags and show additional information. |
| `NoBob` | render | brief: motion blur toggle | Disables the view bobbing effect. |
| `NoFov` | render | brief: FOV changer | — |
| `NoHurtCam` | render | brief: damage tint | Disables the camera effect when getting hurt. |
| `Particles` | render | brief: particle toggle | Displays particles when attacking an entity. |
| `QuickPerspectiveSwap` | render | brief: freelook/perspective | Allows you to quickly change the game's perspective. |
| `SkinChanger` | render | brief: skin manager | Hides the player's real skin client-side (useful for videos). |
| `Sneak` | movement | brief: toggle sneak/sprint. Needs auditing — upstream may bundle sneak-while-moving behaviour. | Automatically makes you sneak constantly. |
| `Sprint` | movement | brief: toggle sneak/sprint. Needs auditing — upstream may bundle omni-sprint behaviour. | Makes you sprint automatically. |
| `TextFieldProtect` | misc | Privacy feature — keeps typed text off screen. Fits the positioning. | Hides rendered text of text field widget when it matches certain patterns. |
| `TNTTimer` | render | brief: TNT countdown overlay | — |
| `TotemEffect` | render | Renders an effect on totem use. Rendering is not automation — auto-totem is the excluded item, this is not it. | Renders an effect when players use a totem of undying. |
| `Wings` | render | brief: cosmetics system (wings) | Render wings behind player |
| `Zoom` | render | brief: smooth zoom | Allows you to make everything in your world appear smaller or bigger. |

---

## Review (12)

Plausible QoL, but either not in the brief or clean-looking with a
cheat-adjacent edge. I did not want to silently decide these.

| Module | Category | The question | Upstream description |
| --- | --- | --- | --- |
| `Animations` | render | Upstream uses this for animation spoofing as well as cosmetics. Salvageable only if the spoofing settings go. | Allows you to modify many of game's animations. |
| `AntiExploit` | player | Defensive: protects against malicious servers. Fits the trust positioning if it is genuinely defensive. | Prevents the server from exploiting client-side bugs. |
| `AutoF5` | render | Auto third-person when opening a container. Convenience, but not in the brief. | Automatically enables F5 mode when opening the inventory or a chest. |
| `AutoRespawn` | player | Automates the respawn button. Trivially QoL, but it is still automation — same category as the excluded auto-totem. | Automatically respawns you after dying. |
| `BetterInventory` | render | "Inventory-related visual features" — needs reading to see whether any of it is automation. | Additional inventory-related visual features. |
| `DamageParticles` | render | Damage numbers. HUD info, but arguably a combat readability advantage. | Show health changes of entities. |
| `GUICloser` | misc | Closes GUIs under some conditions. Could be QoL or could be an anti-detection behaviour. | — |
| `ItemScroller` | misc | Inventory manipulation shortcuts. QoL in most clients; check it is not automated moving. | Quickly moves items in the inventory with the SHIFT and LMB pressed |
| `MiddleClickAction` | misc | Bindable middle-click. Likely fine, overlaps with Macros. | Allows you to perform actions with middle clicks. |
| `NoSwing` | render | Hides the swing animation client-side. Visual only, but it hides your own attacks from you. | Disables the hand swing animation. |
| `Notifier` | misc | Client notifications. Check what it notifies about — upstream may tie it to cheat events. | Notifies you about all kinds of events. |
| `SmoothCamera` | render | Camera smoothing. Reads as cosmetic; verify it does not smooth aim. | Makes your camera move smoother. |

---

## Remove (193)

Cheats, exploits, wallhacks and automation. Whole categories go without
argument: every module under `exploit`, `world`, `fun`, and all of
`combat` bar none.

**combat** — 27 of 27

`Aimbot`, `AutoArmor`, `AutoBow`, `AutoClicker`, `AutoLeave`, `AutoRod`, `AutoShoot`, `AutoWeapon`, `Backtrack`, `Criticals`, `CrystalAura`, `DroneControl`, `ElytraTarget`, `FakeLag`, `Hitbox`, `KeepSprint`, `KillAura`, `MaceKill`, `NoMissCooldown`, `ProjectileAimbot`, `SpearKill`, `SuperKnockback`, `SwordBlock`, `TickBase`, `TimerRange`, `TpAura`, `Velocity`

**exploit** — 25 of 25

`AbortBreaking`, `AntiHunger`, `AntiReducedDebugInfo`, `ClickTp`, `Clip`, `Damage`, `Disabler`, `Dupe`, `ExtendedFirework`, `GhostHand`, `Kick`, `MoreCarry`, `MultiActions`, `NameCollector`, `NoPitchLimit`, `Phase`, `PingSpoof`, `Plugins`, `PortalMenu`, `ResetVL`, `ServerCrasher`, `SleepWalker`, `TimeShift`, `VehicleOneHit`, `YggdrasilSignatureFix`

**fun** — 7 of 7

`DankBobbing`, `Derp`, `HandDerp`, `Notebot`, `SkinDerp`, `Twerk`, `Vomit`

**misc** — 17 of 28

`AntiBot`, `AntiCheatDetect`, `AntiStaff`, `AutoAccount`, `AutoChatGame`, `AutoConfig`, `AutoPearl`, `BookBot`, `DebugRecorder`, `EasyPearl`, `ElytraSwap`, `FlagCheck`, `PacketLogger`, `ReportHelper`, `Spammer`, `TargetLock`, `Teams`

**movement** — 37 of 39

`AirJump`, `Anchor`, `AntiBounce`, `AntiLevitation`, `AutoDodge`, `AvoidHazards`, `BlockBounce`, `BlockWalk`, `ElytraFly`, `ElytraRecast`, `EntityControl`, `Fly`, `Freeze`, `HighJump`, `InventoryMove`, `LiquidWalk`, `LongJump`, `NoClip`, `NoJumpDelay`, `NoPose`, `NoPush`, `NoSlow`, `NoWeb`, `Parkour`, `ReverseStep`, `SafeWalk`, `SnapTap`, `Speed`, `Spider`, `Step`, `Strafe`, `TargetStrafe`, `Teleport`, `TerrainSpeed`, `TridentBoost`, `VehicleBoost`, `VehicleControl`

**player** — 27 of 29

`AntiAFK`, `AntiVoid`, `AutoBreak`, `AutoBuff`, `AutoCrafter`, `AutoFish`, `AutoQueue`, `AutoShop`, `AutoWalk`, `AutoWindCharge`, `Blink`, `ChestCleaner`, `ChestStealer`, `Eagle`, `FastExp`, `FastUse`, `InventoryCleaner`, `NoBlockInteract`, `NoEntityInteract`, `NoFall`, `NoRotateSet`, `NoSlotSet`, `Offhand`, `PotionSpoof`, `Reach`, `Replenish`, `SmartEat`

**render** — 28 of 58

`Aspect`, `BedPlates`, `BlockESP`, `Breadcrumbs`, `CameraClip`, `Chams`, `CombineMobs`, `CrystalView`, `ESP`, `FreeCam`, `HoleESP`, `ItemChams`, `ItemESP`, `LogoffSpot`, `MobOwners`, `MurderMystery`, `NewChunks`, `ProphuntESP`, `ProtectionZones`, `Radar`, `Rotations`, `SilentHotbar`, `StorageESP`, `Tracers`, `Trajectories`, `TrueSight`, `VoidESP`, `XRay`

**world** — 25 of 25

`AirPlace`, `AutoBuild`, `AutoDisable`, `AutoFarm`, `AutoTool`, `AutoTrap`, `BedDefender`, `BlockIn`, `BlockTrap`, `Extinguish`, `FastBreak`, `FastPlace`, `Fucker`, `HoleFiller`, `LiquidFiller`, `LiquidPlace`, `NoInterpolation`, `NoSlowBreak`, `Nuker`, `PacketMine`, `ProjectilePuncher`, `Scaffold`, `StrongholdFinder`, `Surround`, `Timer`

---

## What this does not cover

The module tree is not the whole strip. These also carry cheat machinery and
need their own pass:

- `features/spoofer/` — client/server spoofing
- `deeplearn/` — the TensorFlow rotation system, i.e. aimbot infrastructure
- `features/blink/` — packet delay
- `utils/aiming/` — rotation spoofing used by the combat modules
- `script/` — the ScriptAPI, which can re-add anything removed here unless it
  is constrained or dropped

Removing a module is also not just deleting its file: most register events,
mixins and settings elsewhere. The mixin configs under
`src/main/resources/` need auditing alongside every deletion, or the client
will fail to launch on a missing injection target.
