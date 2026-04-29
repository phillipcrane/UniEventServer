import { useEffect, useState } from 'react';
import { FilterBar } from '../components/FilterBar';
import { ThemeToggle } from '../components/ThemeToggle';
import { EventList } from '../components/EventList';
import { CalendarView } from '../components/Calendar';
import { Footer } from '../components/Footer';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { UserMenu } from '../components/UserMenu';
import { getEvents, getPages } from '../services/dal';
import { getFacebookAuthUrl } from '../services/facebook';
import { parseDateOnly, startOfDayMs, endOfDayMs } from '../utils/dateUtils';
import type { Event as EventType, Page, SortMode } from '../types';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { CalendarDays, LayoutList } from 'lucide-react';

export function MainPage() {
  const { currentUser, logout, isLoading: authLoading } = useAuth();
  const [pages, setPages] = useState([] as Page[]);
  const [events, setEvents] = useState([] as EventType[]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [isSigningOut, setIsSigningOut] = useState(false);
  const [fbConnecting, setFbConnecting] = useState(false);
  const [fbMessage, setFbMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null);

  // organizer filter
  const [pageIds, setPageIds] = useState<string[]>([]);
  const [query, setQuery] = useState<string>('');
  const [debouncedQuery, setDebouncedQuery] = useState<string>('');
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');
  const [sortMode, setSortMode] = useState<SortMode>('upcoming');
  const [viewMode, setViewMode] = useState<'list' | 'calendar'>('list');

  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        setLoading(true);
        const [page, event] = await Promise.all([getPages(), getEvents()]);
        if (cancelled) return;

        setPages(page);
        setEvents(event);
      } catch (err) {
        if (cancelled) return;
        const message =
          err instanceof Error && err.message ? err.message : 'Failed to load data';
        setError(message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const id = setTimeout(() => {
      setDebouncedQuery(query.trim().toLowerCase());
    }, 250);

    return () => clearTimeout(id);
  }, [query]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const success = params.get('success');
    const fbError = params.get('error');
    if (success === 'true') {
      const pages = params.get('pages');
      setFbMessage({ kind: 'success', text: `Facebook connected - ${pages ?? '0'} page(s) linked.` });
      window.history.replaceState({}, '', window.location.pathname);
    } else if (fbError) {
      // Map known OAuth error codes to friendly messages instead of surfacing raw
      // provider strings. Unknown codes get a generic fallback.
      const FB_ERROR_MESSAGES: Record<string, string> = {
        access_denied: 'Facebook access was denied. Please try again and accept the required permissions.',
        server_error: 'Facebook encountered a server error. Please try again later.',
        temporarily_unavailable: 'Facebook is temporarily unavailable. Please try again later.',
      };
      const friendlyError = FB_ERROR_MESSAGES[fbError] ?? 'Could not connect Facebook. Please try again.';
      setFbMessage({ kind: 'error', text: friendlyError });
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, []);

  async function handleFacebookConnect() {
    try {
      setFbConnecting(true);
      setFbMessage(null);
      const url = await getFacebookAuthUrl();
      window.location.href = url;
    } catch (err) {
      setFbMessage({ kind: 'error', text: err instanceof Error ? err.message : 'Could not start Facebook login.' });
    } finally {
      setFbConnecting(false);
    }
  }

  async function handleSignOut() {
    try {
      setIsSigningOut(true);
      setError('');
      await logout();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not sign out.');
    } finally {
      setIsSigningOut(false);
    }
  }

  const userLabel = currentUser?.username || currentUser?.email || 'My Profile';

  const filteredByPage =
    pageIds.length > 0 ? events.filter((e) => pageIds.includes(e.pageId)) : events;

  const textFiltered = debouncedQuery
    ? filteredByPage.filter((event) => {
      const haystack = (
        (event.title || '') +
        ' ' +
        (event.description || '') +
        ' ' +
        (event.place?.name || '')
      ).toLowerCase();

      return haystack.includes(debouncedQuery);
    })
    : filteredByPage;

  const fromObj = parseDateOnly(fromDate);
  const toObj = parseDateOnly(toDate);
  const invalidRange = !!(fromObj && toObj && toObj < fromObj);
  const effectiveToObj = invalidRange ? undefined : toObj;

  const dateFiltered = textFiltered.filter((event) => {
    const eventMs = new Date(event.startTime).getTime();
    if (fromObj && eventMs < startOfDayMs(fromObj)) return false;
    if (effectiveToObj && eventMs > endOfDayMs(effectiveToObj)) return false;
    return true;
  });

  const getCreatedMs = (e: EventType) => {
    type LegacyEvent = EventType & { createdTime?: string; postedTime?: string; insertedAt?: string; addedAt?: string };
    const le = e as LegacyEvent;
    const maybe =
      le.createdTime ??
      le.createdAt ??
      le.postedTime ??
      le.insertedAt ??
      le.addedAt ??
      le.startTime;

    const ms = Date.parse(maybe);
    return isNaN(ms) ? new Date(e.startTime).getTime() : ms;
  };

  let list = [...dateFiltered];

  if (sortMode !== 'all') {
    const now = new Date().getTime();
    list = list.filter((event) => new Date(event.startTime).getTime() >= now);
  }

  if (sortMode === 'upcoming') {
    list = list.sort(
      (a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
    );
  } else if (sortMode === 'newest') {
    list = list.sort((a, b) => getCreatedMs(b) - getCreatedMs(a));
  }

  const count = list.length;

  return (
    <div className="min-h-screen flex flex-col">
      <header className="page-header mx-6 md:mx-8 mt-4 md:mt-6 mb-6">
        <div className="header-content">
          <HeaderLogoLink />
          <div className="header-text">
            <h1 className="header-title main-header-title">DTU Events</h1>
            <p className="header-subtitle main-header-subtitle">Discover Technical University of Denmark Events</p>
          </div>
        </div>

        <div className="header-toggle relative flex items-center gap-2 rounded-xl border border-[var(--panel-border)] bg-[var(--panel-bg)] px-2 py-1.5 shadow-sm">
          <ThemeToggle />

          {currentUser ? (
            <UserMenu
              userLabel={userLabel}
              onSignOut={handleSignOut}
              isSigningOut={isSigningOut || authLoading}
            />
          ) : (
            <Link
              to="/login"
              className="rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-xs font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)] sm:px-4 sm:text-sm"
              aria-label="Log in or sign up"
            >
              Log In / Sign Up
            </Link>
          )}
        </div>
      </header>

      <div className="flex-1 px-6 md:px-8 pb-8 max-w-6xl mx-auto w-full">
        <div className="space-y-8">
          <FilterBar
            pages={pages}
            pageIds={pageIds}
            setPageIds={setPageIds}
            query={query}
            setQuery={setQuery}
            fromDate={fromDate}
            setFromDate={setFromDate}
            toDate={toDate}
            setToDate={setToDate}
            count={count}
            sortMode={sortMode}
            setSortMode={setSortMode}
          />

          {loading && (
            <p className="text-sm text-[var(--text-subtle)] mb-2 animate-pulse">Loading…</p>
          )}

          {error && (
            <p className="text-sm text-[var(--dtu-accent)] mb-2 font-semibold">{error}</p>
          )}

          {invalidRange && (
            <p className="text-xs text-[var(--dtu-accent)] mb-2 font-semibold">
              End date is before start date. Showing results up to any end date.
            </p>
          )}

          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between rounded-xl border border-[var(--panel-border)] bg-[var(--panel-bg)] px-4 py-3 gap-3">
            <div className="text-sm font-medium text-[var(--text-subtle)]" aria-live="polite">
              {count} event{count === 1 ? '' : 's'} found
            </div>

            <div className="inline-flex items-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--input-bg)] p-1">
              <button
                type="button"
                onClick={() => setViewMode('list')}
                aria-pressed={viewMode === 'list'}
                className={`px-4 py-2 rounded-lg border text-sm font-semibold transition ${viewMode === 'list'
                  ? 'bg-[var(--link-primary)] text-white border-transparent'
                  : 'bg-[var(--panel-bg)] text-[var(--text-primary)] border-[var(--panel-border)] hover:bg-[var(--input-bg)]'
                  }`}
              >
                <span className="inline-flex items-center gap-2">
                  <LayoutList size={16} />
                  List
                </span>
              </button>

              <button
                type="button"
                onClick={() => setViewMode('calendar')}
                aria-pressed={viewMode === 'calendar'}
                className={`px-4 py-2 rounded-lg border text-sm font-semibold transition ${viewMode === 'calendar'
                  ? 'bg-[var(--link-primary)] text-white border-transparent'
                  : 'bg-[var(--panel-bg)] text-[var(--text-primary)] border-[var(--panel-border)] hover:bg-[var(--input-bg)]'
                  }`}
              >
                <span className="inline-flex items-center gap-2">
                  <CalendarDays size={16} />
                  Calendar
                </span>
              </button>
            </div>
          </div>

          {viewMode === 'list' ? <EventList list={list} /> : <CalendarView events={list} />}

          <div className="flex flex-col items-center gap-2">
            {fbMessage && (
              <p className={`text-sm font-medium ${fbMessage.kind === 'success' ? 'text-green-600' : 'text-red-600'}`}>
                {fbMessage.text}
              </p>
            )}
            <button
              onClick={handleFacebookConnect}
              disabled={fbConnecting}
              className="bg-[var(--link-primary)] hover:bg-[var(--link-primary-hover)] text-white font-semibold px-6 py-3 rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105 disabled:opacity-60 disabled:cursor-not-allowed disabled:transform-none"
            >
              {fbConnecting ? 'Connecting…' : 'Connect Facebook Page'}
            </button>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
}