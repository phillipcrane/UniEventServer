import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Database, RefreshCw, Send, ShieldCheck, Trash2 } from 'lucide-react';
import { Footer } from '../components/Footer';
import { AdminHeader } from '../components/AdminHeader';
import { useAuth } from '../context/AuthContext';
import {
    clearAdminData,
    ingestAdminPage,
    loadAdminPages,
    mapAdminToolsError,
    refreshAdminToken,
    refreshAllAdminTokens,
    seedAdminData,
    type AdminPageSummary,
} from '../services/adminTools';

type ToolMessage = {
    kind: 'success' | 'error';
    title: string;
    details: string;
};

function statusChipClass(status: string): string {
    const normalized = status.toLowerCase();

    if (normalized.includes('expired') || normalized.includes('missing') || normalized.includes('invalid')) {
        return 'border-red-500/30 bg-red-500/10 text-red-300';
    }

    if (normalized.includes('soon')) {
        return 'border-amber-500/30 bg-amber-500/10 text-amber-200';
    }

    return 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200';
}

function formatSummaryText(summary: { refreshedCount: number; failedCount: number; durationMs: number }): string {
    return `${summary.refreshedCount} refreshed, ${summary.failedCount} failed in ${summary.durationMs}ms`;
}

export function AdminToolsDashboardPage() {
    const navigate = useNavigate();
    const { currentUser } = useAuth();

    const [isAuthorizing, setIsAuthorizing] = useState(true);
    const [isAdmin, setIsAdmin] = useState(false);
    const [pages, setPages] = useState<AdminPageSummary[]>([]);
    const [isLoadingPages, setIsLoadingPages] = useState(true);
    const [pagesError, setPagesError] = useState('');
    const [activeMessage, setActiveMessage] = useState<ToolMessage | null>(null);
    const [isRefreshingAll, setIsRefreshingAll] = useState(false);
    const [isSeeding, setIsSeeding] = useState(false);
    const [isClearing, setIsClearing] = useState(false);

    const isDevToolsVisible = import.meta.env.DEV;

    useEffect(() => {
        if (!currentUser) {
            navigate('/login', { replace: true });
            return;
        }

        if (currentUser.role !== 'admin') {
            navigate('/', { replace: true });
            return;
        }

        setIsAdmin(true);
        setIsAuthorizing(false);
    }, [currentUser, navigate]);

    useEffect(() => {
        if (!isAdmin) {
            return;
        }

        let cancelled = false;

        void (async () => {
            setIsLoadingPages(true);
            setPagesError('');

            try {
                const loadedPages = await loadAdminPages();
                if (cancelled) {
                    return;
                }

                setPages(loadedPages);
            } catch (error) {
                if (cancelled) {
                    return;
                }

                setPages([]);
                setPagesError(mapAdminToolsError(error));
            } finally {
                if (!cancelled) {
                    setIsLoadingPages(false);
                }
            }
        })();

        return () => {
            cancelled = true;
        };
    }, [isAdmin]);

    async function runPageAction(action: 'refresh' | 'ingest', pageId: string) {
        setActiveMessage(null);

        try {
            if (action === 'refresh') {
                const result = await refreshAdminToken(pageId);
                setActiveMessage({
                    kind: result.success ? 'success' : 'error',
                    title: result.success ? 'Token refreshed' : 'Token refresh failed',
                    details: `${result.pageId}: ${result.message}`,
                });
                return;
            }

            const result = await ingestAdminPage(pageId);
            setActiveMessage({
                kind: 'success',
                title: 'Ingest complete',
                details: `${result.pageId}: ${result.eventCount} events imported${result.eventTitles.length ? ` (${result.eventTitles.slice(0, 3).join(', ')}${result.eventTitles.length > 3 ? ', ...' : ''})` : ''}`,
            });
        } catch (error) {
            setActiveMessage({
                kind: 'error',
                title: 'Action failed',
                details: mapAdminToolsError(error),
            });
        }
    }

    async function handleRefreshAll() {
        setIsRefreshingAll(true);
        setActiveMessage(null);

        try {
            const summary = await refreshAllAdminTokens();
            setActiveMessage({
                kind: summary.failedCount === 0 ? 'success' : 'error',
                title: 'Refreshed all tokens',
                details: formatSummaryText(summary),
            });
        } catch (error) {
            setActiveMessage({
                kind: 'error',
                title: 'Refresh failed',
                details: mapAdminToolsError(error),
            });
        } finally {
            setIsRefreshingAll(false);
        }
    }

    async function handleSeedData() {
        setIsSeeding(true);
        setActiveMessage(null);

        try {
            const result = await seedAdminData();
            setActiveMessage({
                kind: result.success ? 'success' : 'error',
                title: result.success ? 'Seeded test data' : 'Seeding failed',
                details: `${result.message} Pages: ${result.pageCount}, events: ${result.eventCount}, places: ${result.placeCount}`,
            });
        } catch (error) {
            setActiveMessage({
                kind: 'error',
                title: 'Seeding failed',
                details: mapAdminToolsError(error),
            });
        } finally {
            setIsSeeding(false);
        }
    }

    async function handleClearSeedData() {
        setIsClearing(true);
        setActiveMessage(null);

        try {
            const result = await clearAdminData();
            setActiveMessage({
                kind: result.success ? 'success' : 'error',
                title: result.success ? 'Cleared seeded data' : 'Clear failed',
                details: `${result.message} Pages: ${result.pageCount}, events: ${result.eventCount}, places: ${result.placeCount}`,
            });
        } catch (error) {
            setActiveMessage({
                kind: 'error',
                title: 'Clear failed',
                details: mapAdminToolsError(error),
            });
        } finally {
            setIsClearing(false);
        }
    }

    if (isAuthorizing) {
        return (
            <div className="min-h-screen flex flex-col">
                <AdminHeader
                    title="Admin Dashboard"
                    subtitle="Run the server-side admin tools"
                    backTo="/profile"
                    backLabel="Back to Profile"
                />
                <main className="flex-1 px-6 md:px-8 pb-12 max-w-6xl mx-auto w-full">
                    <div className="rounded-2xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 shadow-lg">
                        <p className="text-sm text-[var(--text-subtle)]">Checking admin access...</p>
                    </div>
                </main>
                <Footer />
            </div>
        );
    }

    if (!currentUser || !isAdmin) {
        return null;
    }

    return (
        <div className="min-h-screen flex flex-col">
            <AdminHeader
                title="Admin Dashboard"
                subtitle="Run the server-side admin tools"
                backTo="/profile"
                backLabel="Back to Profile"
            />

            <main className="flex-1 px-6 md:px-8 pb-12 max-w-6xl mx-auto w-full space-y-6">
                <section className="grid gap-4 lg:grid-cols-3">
                    <div className="rounded-2xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 shadow-lg lg:col-span-2">
                        <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[var(--text-subtle)]">Quick Start</p>
                        <h2 className="mt-1 text-2xl font-bold text-[var(--text-primary)]">Admin tools at a glance</h2>
                        <p className="mt-2 text-sm text-[var(--text-subtle)]">
                            Keep this page simple: choose a page, refresh its token, ingest events, or jump to organizer key generation.
                        </p>

                        <div className="mt-5 flex flex-wrap gap-3">
                            <Link
                                to="/admin/generate-organizer-key"
                                className="inline-flex items-center gap-2 rounded-lg bg-[var(--link-primary)] px-4 py-2.5 text-sm font-semibold text-white transition-colors duration-200 hover:bg-[var(--link-primary-hover)]"
                            >
                                <ShieldCheck size={16} />
                                Generate organizer key
                            </Link>
                            <button
                                type="button"
                                onClick={handleRefreshAll}
                                disabled={isRefreshingAll}
                                className="inline-flex items-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-4 py-2.5 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)] disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                <RefreshCw size={16} />
                                {isRefreshingAll ? 'Refreshing...' : 'Refresh all tokens'}
                            </button>
                        </div>
                    </div>

                    <div className="rounded-2xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 shadow-lg">
                        <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[var(--text-subtle)]">Maintenance</p>
                        <h2 className="mt-1 text-2xl font-bold text-[var(--text-primary)]">Seed data</h2>
                        <p className="mt-2 text-sm text-[var(--text-subtle)]">
                            Development-only helpers for quickly resetting the local database.
                        </p>

                        <div className="mt-5 space-y-3">
                            <button
                                type="button"
                                onClick={handleSeedData}
                                disabled={!isDevToolsVisible || isSeeding}
                                className="inline-flex w-full items-center justify-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)] disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                <Database size={16} />
                                {isSeeding ? 'Seeding...' : 'Seed demo data'}
                            </button>
                            <button
                                type="button"
                                onClick={handleClearSeedData}
                                disabled={!isDevToolsVisible || isClearing}
                                className="inline-flex w-full items-center justify-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)] disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                <Trash2 size={16} />
                                {isClearing ? 'Clearing...' : 'Clear seeded data'}
                            </button>
                        </div>

                        {!isDevToolsVisible && (
                            <p className="mt-3 text-xs text-[var(--text-subtle)]">These buttons are only enabled in local development.</p>
                        )}
                    </div>
                </section>

                {activeMessage && (
                    <section
                        role="status"
                        className={`rounded-2xl border p-5 shadow-lg ${activeMessage.kind === 'success' ? 'border-emerald-500/30 bg-emerald-500/10' : 'border-red-500/30 bg-red-500/10'}`}
                    >
                        <p className="text-sm font-semibold text-[var(--text-primary)]">{activeMessage.title}</p>
                        <p className="mt-1 text-sm text-[var(--text-subtle)]">{activeMessage.details}</p>
                    </section>
                )}

                <section className="rounded-2xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 shadow-lg">
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                        <div>
                            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[var(--text-subtle)]">Tracked Pages</p>
                            <h2 className="mt-1 text-2xl font-bold text-[var(--text-primary)]">Facebook pages</h2>
                            <p className="mt-2 text-sm text-[var(--text-subtle)]">Run ingest or token refresh for any tracked page.</p>
                        </div>
                        <div className="text-sm text-[var(--text-subtle)]">
                            {isLoadingPages ? 'Loading pages...' : `${pages.length} pages loaded`}
                        </div>
                    </div>

                    {pagesError && (
                        <div className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200" role="alert">
                            {pagesError}
                        </div>
                    )}

                    {!pagesError && !isLoadingPages && pages.length === 0 && (
                        <p className="mt-4 text-sm text-[var(--text-subtle)]">No pages were returned by the backend.</p>
                    )}

                    <div className="mt-5 space-y-3">
                        {pages.map((page) => (
                            <article key={page.id} className="rounded-2xl border border-[var(--panel-border)] bg-[var(--input-bg)]/60 p-4">
                                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                                    <div className="space-y-2">
                                        <div className="flex flex-wrap items-center gap-3">
                                            <h3 className="text-lg font-bold text-[var(--text-primary)]">{page.name}</h3>
                                            <span className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.08em] ${statusChipClass(page.tokenStatus)}`}>
                                                {page.tokenStatus}
                                            </span>
                                        </div>
                                        <p className="text-sm text-[var(--text-subtle)]">ID: {page.id}</p>
                                        <p className="text-sm text-[var(--text-subtle)]">
                                            Token expires in{' '}
                                            {page.tokenExpiresInDays === null ? 'unknown' : `${page.tokenExpiresInDays} day${page.tokenExpiresInDays === 1 ? '' : 's'}`}
                                        </p>
                                    </div>

                                    <div className="flex flex-wrap gap-2">
                                        <button
                                            type="button"
                                            onClick={() => { void runPageAction('refresh', page.id); }}
                                            className="inline-flex items-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)]"
                                        >
                                            <RefreshCw size={16} />
                                            Refresh token
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => { void runPageAction('ingest', page.id); }}
                                            className="inline-flex items-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)]"
                                        >
                                            <Send size={16} />
                                            Ingest events
                                        </button>
                                    </div>
                                </div>
                            </article>
                        ))}
                    </div>
                </section>
            </main>

            <Footer />
        </div>
    );
}