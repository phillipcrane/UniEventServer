export function ChunkErrorFallback() {
  return (
    <div className="flex items-center justify-center min-h-screen bg-red-50 dark:bg-red-950">
      <div className="text-center p-6">
        <h1 className="text-2xl font-bold text-red-900 dark:text-red-100 mb-2">
          Loading Error
        </h1>
        <p className="text-red-800 dark:text-red-200 mb-4">
          Failed to load this page. Please try refreshing or going back.
        </p>
        <div className="flex gap-4 justify-center">
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
          >
            Refresh Page
          </button>
          <button
            onClick={() => window.history.back()}
            className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700"
          >
            Go Back
          </button>
        </div>
      </div>
    </div>
  );
}
