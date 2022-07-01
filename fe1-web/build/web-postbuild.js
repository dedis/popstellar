const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const gitHash = execSync('git rev-parse HEAD', { encoding: 'ascii' }).trim();
const isDirty = execSync("git diff --quiet || echo '-dirty'", { encoding: 'ascii' }).trim();

const oldName = path.resolve(__dirname, '..', 'dist', 'fe1.zip');
const newName = path.resolve(__dirname, '..', 'dist', `fe1-${gitHash}${isDirty}-${Date.now()}.zip`);

fs.renameSync(oldName, newName);
