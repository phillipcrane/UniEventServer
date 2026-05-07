import { ChunkErrorFallback } from '../components/ChunkErrorFallback';
import { ChunkLoadingFallback } from '../components/ChunkLoadingFallback';

export function ChunkFallbackPreviewPage() {
  return (
    <div className="min-h-screen bg-slate-950 px-6 py-10 text-slate-100">
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-cyan-300">Preview</p>
          <h1 className="mt-2 text-3xl font-bold text-white">Chunk fallback components</h1>
          <p className="mt-3 max-w-2xl text-slate-300">
            This page renders the loading and error states directly so you can inspect them in the browser.
          </p>
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          <section className="overflow-hidden rounded-3xl border border-white/10 bg-white/5 shadow-2xl shadow-cyan-950/30">
            <div className="border-b border-white/10 px-5 py-3 text-sm font-medium text-slate-300">
              Loading fallback
            </div>
            <div className="min-h-[32rem] bg-white text-slate-900">
              <ChunkLoadingFallback />
            </div>
          </section>

          <section className="overflow-hidden rounded-3xl border border-white/10 bg-white/5 shadow-2xl shadow-cyan-950/30">
            <div className="border-b border-white/10 px-5 py-3 text-sm font-medium text-slate-300">
              Error fallback
            </div>
            <div className="min-h-[32rem] bg-white text-slate-900">
              <ChunkErrorFallback />
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}