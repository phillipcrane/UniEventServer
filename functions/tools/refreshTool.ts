async function runRefreshTool() {
  console.log('Running manual token refresh tool...');
  try {
    const response = await fetch('https://europe-west1-dtuevent-8105b.cloudfunctions.net/handleRefreshTokens', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    const result = await response.json();
    console.log('Token refresh finished:', result);
  } catch (error) {
    console.error('Refresh tool failed:', error instanceof Error ? error.message : error);
    process.exitCode = 1;
  }
}

runRefreshTool();