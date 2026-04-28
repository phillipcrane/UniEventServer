import { FilterBar } from '../components/FilterBar';
import { ThemeToggle } from '../components/ThemeToggle';
import { EventList } from '../components/EventList';
import { CalendarView } from '../components/Calendar';
import { Footer } from '../components/Footer';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { UserMenu } from '../components/UserMenu';
import { useMainPage } from '../hooks/useMainPage';
import { Link } from 'react-router-dom';
import { CalendarDays, LayoutList } from 'lucide-react';

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
    pageIds, setPageIds,
    query, setQuery,
    fromDate, setFromDate,
    toDate, setToDate,
    sortMode, setSortMode,
    viewMode, setViewMode,
    count,
    invalidRange,
    userLabel,
    handleFacebookConnect,
    handleSignOut,
  } = useMainPage();

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
              isSigningOut={isSigningOut}
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

          {currentUser?.role === 'organizer' && (
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
          )}
        </div>
      </div>

      <Footer />
    </div>
  );
}
