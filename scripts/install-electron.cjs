// Electron 44 makes runtime download an explicit install step.
const { execFileSync } = require('node:child_process');
execFileSync(process.execPath, [require.resolve('electron/install.js')], { stdio: 'inherit' });
