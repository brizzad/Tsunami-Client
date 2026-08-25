import fs from "fs";
import path from "path";

const ROOT = process.cwd();
const MODULES = path.join(ROOT, "src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules");
const LANG = path.join(ROOT, "src/main/resources/resources/liquidbounce/lang/en_us.json");

const lang = JSON.parse(fs.readFileSync(LANG, "utf8"));

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (/^Module[A-Za-z0-9]+\.kt$/.test(e.name)) out.push(p);
  }
  return out;
}

const rows = [];
for (const category of fs.readdirSync(MODULES)) {
  const dir = path.join(MODULES, category);
  if (!fs.statSync(dir).isDirectory()) continue;
  for (const file of walk(dir)) {
    const name = path.basename(file, ".kt").replace(/^Module/, "");
    const key = name.charAt(0).toLowerCase() + name.slice(1);
    const desc = lang[`liquidbounce.module.${key}.description`] || "";
    const lines = fs.readFileSync(file, "utf8").split("\n").length;
    rows.push({ category, name, desc, lines });
  }
}

rows.sort((a, b) => a.category.localeCompare(b.category) || a.name.localeCompare(b.name));

const out = rows
  .map((r) => `${r.category}\t${r.name}\t${r.lines}\t${r.desc.replace(/\s+/g, " ")}`)
  .join("\n");

fs.writeFileSync(process.argv[2], "category\tmodule\tlines\tdescription\n" + out);

const byCat = {};
for (const r of rows) byCat[r.category] = (byCat[r.category] || 0) + 1;
console.log("total modules:", rows.length);
console.log(Object.entries(byCat).map(([k, v]) => `  ${k.padEnd(10)} ${v}`).join("\n"));
console.log("missing descriptions:", rows.filter((r) => !r.desc).length);
