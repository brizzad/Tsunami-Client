import fs from "fs";

const tsv = fs.readFileSync(process.argv[2], "utf8").trim().split("\n").slice(1);
const rows = tsv.map((l) => {
  const [category, module, lines, description] = l.split("\t");
  return { category, module, lines: +lines, description: description || "" };
});

// KEEP: maps onto a feature the Tsunami brief actually asks for, or is
// framework the client cannot run without. Value is the justification.
const KEEP = {
  ClickGui: "The module/settings GUI. Core framework — the reason LiquidBounce was chosen as the base.",
  Hud: "The in-game HUD framework. Everything in the HUD & Visual scope hangs off this.",
  Debug: "Developer tooling. Harmless and useful while building the fork.",

  BlockOutline: "brief: block outline/overlay",
  Crosshair: "brief: crosshair customization",
  CustomAmbience: "brief: weather/time changer",
  FreeLook: "brief: freelook/perspective",
  QuickPerspectiveSwap: "brief: freelook/perspective",
  FullBright: "brief: fullbright/gamma",
  HitFX: "brief: hit color",
  ItemTags: "brief: item counter/tracker",
  Nametags: "brief: titles/name tag customization",
  NoBob: "brief: motion blur toggle",
  NoFov: "brief: FOV changer",
  NoHurtCam: "brief: damage tint",
  Particles: "brief: particle toggle",
  SkinChanger: "brief: skin manager",
  TNTTimer: "brief: TNT countdown overlay",
  Zoom: "brief: smooth zoom",
  AntiBlind: "brief: fire overlay reduction — visibility while burning (toggle)",
  Hats: "brief: cosmetics system (hats)",
  Wings: "brief: cosmetics system (wings)",
  TotemEffect: "Renders an effect on totem use. Rendering is not automation — auto-totem is the excluded item, this is not it.",
  JumpEffect: "Cosmetic only.",

  BetterChat: "brief: chat customization",
  BetterTab: "brief: boss bar/scoreboard/tab list customization",
  BetterTitle: "brief: titles customization",
  Macros: "brief: macros/keybinds",
  NameProtect: "brief: nick hider",
  InventoryTracker: "brief: item physics/counter/tracker",
  TextFieldProtect: "Privacy feature — keeps typed text off screen. Fits the positioning.",

  Sprint: "brief: toggle sneak/sprint. Needs auditing — upstream may bundle omni-sprint behaviour.",
  Sneak: "brief: toggle sneak/sprint. Needs auditing — upstream may bundle sneak-while-moving behaviour.",
};

// REVIEW: plausible QoL but not clearly in the brief, or clean-looking with a
// cheat-adjacent edge. Nathan's call, not mine.
const REVIEW = {
  Animations: "Upstream uses this for animation spoofing as well as cosmetics. Salvageable only if the spoofing settings go.",
  AutoF5: "Auto third-person when opening a container. Convenience, but not in the brief.",
  BetterInventory: "\"Inventory-related visual features\" — needs reading to see whether any of it is automation.",
  DamageParticles: "Damage numbers. HUD info, but arguably a combat readability advantage.",
  SmoothCamera: "Camera smoothing. Reads as cosmetic; verify it does not smooth aim.",
  NoSwing: "Hides the swing animation client-side. Visual only, but it hides your own attacks from you.",
  GUICloser: "Closes GUIs under some conditions. Could be QoL or could be an anti-detection behaviour.",
  ItemScroller: "Inventory manipulation shortcuts. QoL in most clients; check it is not automated moving.",
  MiddleClickAction: "Bindable middle-click. Likely fine, overlaps with Macros.",
  Notifier: "Client notifications. Check what it notifies about — upstream may tie it to cheat events.",
  AntiExploit: "Defensive: protects against malicious servers. Fits the trust positioning if it is genuinely defensive.",
  AutoRespawn: "Automates the respawn button. Trivially QoL, but it is still automation — same category as the excluded auto-totem.",
};

const classify = (m) =>
  KEEP[m.module] ? "KEEP" : REVIEW[m.module] ? "REVIEW" : "REMOVE";

for (const r of rows) r.verdict = classify(r);

const keep = rows.filter((r) => r.verdict === "KEEP");
const review = rows.filter((r) => r.verdict === "REVIEW");
const remove = rows.filter((r) => r.verdict === "REMOVE");

const sum = (a) => a.reduce((n, r) => n + r.lines, 0);
const pct = (n) => ((n / rows.length) * 100).toFixed(0);

// Sanity: every KEEP/REVIEW name must correspond to a real module.
const names = new Set(rows.map((r) => r.module));
const bogus = [...Object.keys(KEEP), ...Object.keys(REVIEW)].filter((n) => !names.has(n));
if (bogus.length) {
  console.error("classification names not found in the tree: " + bogus.join(", "));
  process.exit(1);
}

let md = `# Module inventory

Upstream ships **${rows.length} modules**. This is every one of them, sorted
into what Tsunami keeps, what needs a decision, and what goes.

Generated from the module tree joined against
\`src/main/resources/resources/liquidbounce/lang/en_us.json\`, so the
descriptions are upstream's own words rather than my reading of the class
names. Regenerate with \`scripts/inventory.mjs\` after any upstream merge.

**This is a proposal, not a done deal.** Scope is Nathan's call — the REVIEW
section exists because those are genuinely arguable, and anything in KEEP can
be argued back out.

| Verdict | Modules | Share | Lines |
| --- | ---: | ---: | ---: |
| Keep | ${keep.length} | ${pct(keep.length)}% | ${sum(keep).toLocaleString()} |
| Review | ${review.length} | ${pct(review.length)}% | ${sum(review).toLocaleString()} |
| Remove | ${remove.length} | ${pct(remove.length)}% | ${sum(remove).toLocaleString()} |

Roughly ${pct(remove.length)}% of the module surface goes. That is the fork in
one number: Tsunami is not LiquidBounce with a new coat of paint, it is the
framework plus a different ${keep.length + review.length} modules.

---

## Keep (${keep.length})

Each of these maps onto something the brief actually asks for, or is framework
the client cannot run without.

| Module | Category | Why | Upstream description |
| --- | --- | --- | --- |
`;

for (const r of keep.sort((a, b) => a.module.localeCompare(b.module))) {
  md += `| \`${r.module}\` | ${r.category} | ${KEEP[r.module]} | ${r.description || "—"} |\n`;
}

md += `
---

## Review (${review.length})

Plausible QoL, but either not in the brief or clean-looking with a
cheat-adjacent edge. I did not want to silently decide these.

| Module | Category | The question | Upstream description |
| --- | --- | --- | --- |
`;

for (const r of review.sort((a, b) => a.module.localeCompare(b.module))) {
  md += `| \`${r.module}\` | ${r.category} | ${REVIEW[r.module]} | ${r.description || "—"} |\n`;
}

md += `
---

## Remove (${remove.length})

Cheats, exploits, wallhacks and automation. Whole categories go without
argument: every module under \`exploit\`, \`world\`, \`fun\`, and all of
\`combat\` bar none.

`;

const byCat = {};
for (const r of remove) (byCat[r.category] ||= []).push(r);

for (const cat of Object.keys(byCat).sort()) {
  const list = byCat[cat];
  const total = rows.filter((r) => r.category === cat).length;
  md += `**${cat}** — ${list.length} of ${total}\n\n`;
  md += list.map((r) => `\`${r.module}\``).sort().join(", ") + "\n\n";
}

md += `---

## What this does not cover

The module tree is not the whole strip. These also carry cheat machinery and
need their own pass:

- \`features/spoofer/\` — client/server spoofing
- \`deeplearn/\` — the TensorFlow rotation system, i.e. aimbot infrastructure
- \`features/blink/\` — packet delay
- \`utils/aiming/\` — rotation spoofing used by the combat modules
- \`script/\` — the ScriptAPI, which can re-add anything removed here unless it
  is constrained or dropped

Removing a module is also not just deleting its file: most register events,
mixins and settings elsewhere. The mixin configs under
\`src/main/resources/\` need auditing alongside every deletion, or the client
will fail to launch on a missing injection target.
`;

fs.writeFileSync(process.argv[3], md);
console.log(`keep ${keep.length}  review ${review.length}  remove ${remove.length}  (of ${rows.length})`);
console.log(`lines: keep ${sum(keep)}  review ${sum(review)}  remove ${sum(remove)}`);
