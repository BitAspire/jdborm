#!/usr/bin/env node
const fs = require("fs");
const path = require("path");

const targetDir = path.join(process.cwd(), ".agents", "skills", "jdborm");
const targetFile = path.join(targetDir, "SKILL.md");
const sourceFile = path.join(__dirname, "SKILL.md");

if (!fs.existsSync(targetDir)) {
  fs.mkdirSync(targetDir, { recursive: true });
}

fs.copyFileSync(sourceFile, targetFile);
console.log("✓ jdborm AI skill installed to " + targetFile);
console.log("  AI coding assistants will now understand the jdborm API.");
