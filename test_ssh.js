const { execSync } = require('child_process');
try {
  const stdout = execSync('ssh -o BatchMode=yes -l heuscher devzone.ch', { encoding: 'utf8', stdio: 'pipe' });
  console.log('STDOUT:', stdout);
} catch (e) {
  console.error('ERROR:', e.message);
  console.error('STDERR:', e.stderr);
  console.error('STDOUT:', e.stdout);
}
