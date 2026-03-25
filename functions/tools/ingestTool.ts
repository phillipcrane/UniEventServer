async function runIngestTool() {
  console.log('Running manual ingest tool...');
  try {
    const response = await fetch('https://europe-west1-dtuevent-8105b.cloudfunctions.net/handleManualIngest', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    const result = await response.json();
    console.log('Ingest finished:', result);
  } catch (error) {
    console.error('Ingest tool failed:', error instanceof Error ? error.message : error);
    process.exitCode = 1;
  }
}

runIngestTool();