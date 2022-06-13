const path = require('path');
const fs = require('fs');
const { execSync } = require('child_process');

const gitHash = execSync('git rev-parse HEAD', { encoding: 'ascii' }).trim();
const isDirty = execSync("git diff --quiet || echo '-dirty'", { encoding: 'ascii' }).trim();

const oldName = path.resolve(__dirname, '..', 'dist', 'fe1.zip');
const newName = path.resolve(__dirname, '..', 'dist', `fe1-${gitHash}${isDirty}.zip`);

fs.renameSync(oldName, newName);
