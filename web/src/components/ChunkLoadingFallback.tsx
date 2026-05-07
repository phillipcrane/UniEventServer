export function ChunkLoadingFallback() {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-current"></div>
        <p className="mt-4 text-gray-600 dark:text-gray-400">Loading...</p>
      </div>
    </div>
  );
}
