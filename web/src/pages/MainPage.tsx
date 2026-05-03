import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { FilterBar } from '../components/FilterBar';
import { ThemeToggle } from '../components/ThemeToggle';
import { EventList } from '../components/EventList';
import { CalendarView } from '../components/Calendar';
import { Footer } from '../components/Footer';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { useMainPage } from '../hooks/useMainPage';
import { CalendarDays, CircleUserRound, LayoutList, LogOut } from 'lucide-react';

export function MainPage() {
  const {
    currentUser,
    pages,
    list,
    loading,
    error,
    isSigningOut,
    fbConnecting,
    fbMessage,
    pageIds,
    setPageIds,
    query,
    setQuery,
    fromDate,
    setFromDate,
    toDate,
    setToDate,
    sortMode,
    setSortMode,
    viewMode,
    setViewMode,
    count,
    invalidRange,
    userLabel,
    handleFacebookConnect,
    handleSignOut,
  } = useMainPage();

  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const profileMenuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isProfileOpen) return;

    function handleClickOutside(event: MouseEvent) {
      if (profileMenuRef.current && !profileMenuRef.current.contains(event.target as Node)) {
        setIsProfileOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isProfileOpen]);

  return (
    <div className="min-h-screen flex flex-col">
      <header className="page-header mx-6 md:mx-8 mt-4 md:mt-6 mb-6">
        <div className="header-content">
          <HeaderLogoLink />
          <div className="header-text">
            <h1 className="header-title main-header-title">UniEvent</h1>
            <p className="header-subtitle main-header-subtitle">Discover university and student events</p>
          </div>
        </div>

        <div className="header-toggle relative flex items-center gap-2 rounded-xl border border-[var(--panel-border)] bg-[var(--panel-bg)] px-2 py-1.5 shadow-sm">
          <ThemeToggle />

          {currentUser ? (
            <div className="relative" ref={profileMenuRef}>
              <button
                type="button"
                onClick={() => setIsProfileOpen((open) => !open)}
                className="inline-flex items-center justify-center rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--input-focus-border)]"
                aria-label="Open account menu"
                aria-expanded={isProfileOpen}
              >
                <CircleUserRound size={18} />
              </button>

              {isProfileOpen && (
                <div className="absolute right-0 z-20 mt-2 w-64 rounded-xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-2 shadow-xl">
                  <p className="px-2 py-2 text-xs font-semibold text-[var(--text-subtle)]">Signed in as</p>
                  <p className="truncate px-2 pb-2 text-sm font-semibold text-[var(--text-primary)]">{userLabel}</p>
                  <Link
                    to="/profile"
                    onClick={() => setIsProfileOpen(false)}
                    className="mt-1 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)]"
                  >
                    <CircleUserRound size={16} />
                    Profile
                  </Link>
                  <button
                    type="button"
                    onClick={handleSignOut}
                    disabled={isSigningOut}
                    className="mt-2 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)] disabled:cursor-not-allowed disabled:opacity-70"
                  >
                    <LogOut size={16} />
                    {isSigningOut ? 'Signing out...' : 'Log out'}
                  </button>
                </div>
              )}
            </div>
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
          {currentUser?.role === 'organizer' && (
            <section className="rounded-xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-4 shadow-sm">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[var(--text-subtle)]">Organizer tools</p>
                  <h2 className="mt-1 text-lg font-bold text-[var(--text-primary)]">Connect Facebook to keep events in sync</h2>
                  <p className="mt-1 text-sm text-[var(--text-subtle)]">Use the organizer flow if you need to link a page and refresh event imports.</p>
                </div>

                <button
                  type="button"
                  onClick={handleFacebookConnect}
                  disabled={fbConnecting}
                  className="inline-flex items-center justify-center rounded-lg bg-[var(--link-primary)] px-4 py-2 text-sm font-semibold text-white transition-colors duration-200 hover:bg-[var(--link-primary-hover)] disabled:cursor-not-allowed disabled:opacity-70"
                >
                  {fbConnecting ? 'Connecting...' : 'Connect Facebook'}
                </button>
              </div>

              {fbMessage && (
                <p className={`mt-3 text-sm font-semibold ${fbMessage.kind === 'error' ? 'text-[var(--dtu-accent)]' : 'text-emerald-500'}`} aria-live="polite">
                  {fbMessage.text}
                </p>
              )}
            </section>
          )}

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

          {loading && <p className="text-sm text-[var(--text-subtle)] mb-2 animate-pulse">Loading…</p>}
          {error && <p className="text-sm text-[var(--dtu-accent)] mb-2 font-semibold">{error}</p>}

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
        </div>
      </div>

      <Footer />
    </div>
  );
}
