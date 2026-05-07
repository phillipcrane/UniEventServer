import { Link } from 'react-router-dom';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { ThemeToggle } from '../components/ThemeToggle';
import { Footer } from '../components/Footer';
import { LikeButton } from '../components/LikeButton';
import { useProfilePage } from '../hooks/useProfilePage';
import { formatEventStart } from '../utils/eventUtils';
import type { Event as EventType } from '../types';
import { CalendarDays, CircleUserRound, Heart, LogOut, MapPin, Ticket } from 'lucide-react';

function SavedEventCard({ event }: { event: EventType }) {
  return (
    <article className="group overflow-hidden rounded-2xl border border-[var(--panel-border)] bg-[var(--input-bg)]/75 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl focus-within:ring-2 focus-within:ring-[var(--input-focus-border)]">
      <Link to={`/events/${event.id}`} className="block h-40 overflow-hidden" aria-label={`Open event ${event.title}`}>
        {event.coverImageUrl ? (
          <img
            src={event.coverImageUrl}
            alt={event.title}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
          ) : (
          <div className="flex h-full w-full items-center justify-center bg-[linear-gradient(135deg,var(--profile-fallback-gradient-start),var(--profile-fallback-gradient-end))]">
            <CalendarDays size={26} className="text-white/80" />
          </div>
        )}
      </Link>

      <div className="space-y-3 p-4">
        <div className="flex items-center justify-between gap-2">
          <span className="inline-flex items-center gap-2 rounded-full border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-1 text-xs font-semibold text-[var(--text-primary)]">
            <Heart size={12} fill="currentColor" />
            Saved
          </span>
          <LikeButton event={event} compact />
        </div>

        <Link
          to={`/events/${event.id}`}
          className="block rounded-md text-lg font-bold text-[var(--text-primary)] transition hover:text-[var(--link-primary)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--input-focus-border)]"
        >
          {event.title}
        </Link>

        <div className="space-y-2 text-sm text-[var(--text-subtle)]">
          <div className="flex items-center gap-2">
            <CalendarDays size={14} />
            <span>{formatEventStart(event.startTime)}</span>
          </div>
          <div className="flex items-center gap-2">
            <MapPin size={14} />
            <span>{event.place?.name || 'Location TBA'}</span>
          </div>
          <div className="flex items-center gap-2">
            <Ticket size={14} />
            <span>Saved to your profile</span>
          </div>
        </div>
      </div>
    </article>
  );
}

export function ProfilePage() {
  const {
    currentUser,
    accountRole,
    organizerNames,
    isSigningOut,
    fbConnecting,
    fbError,
    isLoadingLikedEvents,
    likedEvents,
    userLabel,
    username,
    profileImage,
    handleFacebookConnect,
    handleSignOut,
  } = useProfilePage();

  return (
    <div className="min-h-screen flex flex-col">
      <header className="page-header mx-6 md:mx-8 mt-4 md:mt-6 mb-8">
        <div className="header-content">
          <HeaderLogoLink />
          <div className="header-text profile-header-text">
            <h1 className="header-title">Profile</h1>
            <p className="header-subtitle">Manage your account and saved events</p>
          </div>
        </div>

        <div className="header-toggle relative flex items-center gap-2 rounded-xl border border-[var(--panel-border)] bg-[var(--panel-bg)] px-2 py-1.5 shadow-sm">
          <ThemeToggle />
          <button
            type="button"
            onClick={handleSignOut}
            disabled={isSigningOut}
            aria-label="Log out"
            title="Log out"
            className="inline-flex items-center justify-center rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] p-2 text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--input-focus-border)] disabled:cursor-not-allowed disabled:opacity-70"
          >
            <LogOut size={16} />
          </button>
        </div>
      </header>

      <main className="flex-1 px-6 md:px-8 pb-12 max-w-6xl mx-auto w-full">
        <section aria-label="Profile overview" className="rounded-2xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 shadow-lg">
          <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-start">
            <div className="relative flex h-36 w-36 flex-shrink-0 items-center justify-center overflow-hidden rounded-full border-4 border-[var(--dtu-accent-light)] bg-[var(--profile-avatar-bg)] shadow-[0_0_0_8px_var(--profile-avatar-ring)]">
              {profileImage ? (
                <img src={profileImage} alt={userLabel} className="h-full w-full object-cover" />
              ) : (
                <CircleUserRound aria-label="Default profile picture" className="h-[86%] w-[86%] text-[var(--profile-avatar-icon-color)]" strokeWidth={1.55} />
              )}
            </div>

            <div className="flex-1 space-y-4 text-center sm:text-left">
              <div>
                <h2 className="text-3xl font-bold text-[var(--text-primary)]">{username}</h2>
                <span className={`mt-2 inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.1em] ${accountRole === 'organizer' || accountRole === 'admin'
                  ? 'border border-transparent bg-[var(--link-primary)] text-white'
                  : 'border border-[var(--panel-border)] bg-[var(--panel-bg)] text-[var(--text-primary)]'
                  }`}>
                  {accountRole === 'admin' ? 'Admin' : accountRole === 'organizer' ? 'Organizer' : 'User'}
                </span>
              </div>

              <div className="space-y-1">
                <p className="text-base font-semibold text-[var(--text-primary)]">{userLabel}</p>
                <p className="text-sm text-[var(--text-subtle)]">{currentUser?.email || 'No email available'}</p>
              </div>

              <div className="flex flex-wrap items-center justify-center gap-2 sm:justify-start">
                <span className="inline-flex items-center gap-2 rounded-full border border-[var(--panel-border)] bg-[var(--input-bg)] px-3 py-1 text-xs font-semibold text-[var(--text-subtle)]">
                  <Heart size={12} fill="currentColor" />
                  {likedEvents.length} saved
                </span>
                {accountRole === 'admin' && (
                    <Link
                    to="/admin"
                        className="inline-flex items-center gap-2 rounded-full border border-[var(--admin-role-border)] bg-[var(--admin-role-bg)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.08em] text-[var(--admin-role-text)] transition-colors duration-200 hover:bg-[var(--admin-role-bg-hover)]"
                    >
                    Admin Dashboard
                    </Link>
                )}
              </div>
            </div>
          </div>
        </section>

        {(accountRole === 'organizer' || accountRole === 'admin') && (
          <section className="mt-6 rounded-2xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 shadow-lg">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[var(--text-subtle)]">Facebook Integration</p>
                <h3 className="mt-1 text-lg font-bold text-[var(--text-primary)]">Connect your Facebook Page</h3>
                {organizerNames.length > 0 && (
                  <p className="mt-1 text-sm text-[var(--text-subtle)]">Connected organizers: {organizerNames.join(', ')}</p>
                )}
              </div>

              <button
                type="button"
                onClick={handleFacebookConnect}
                disabled={fbConnecting}
                className="inline-flex items-center justify-center rounded-lg bg-[var(--link-primary)] px-6 py-3 text-sm font-semibold text-white transition-colors duration-200 hover:bg-[var(--link-primary-hover)] disabled:cursor-not-allowed disabled:opacity-70"
              >
                {fbConnecting ? 'Connecting...' : 'Connect Facebook Page'}
              </button>
            </div>

            {fbError && <p className="mt-3 text-sm font-semibold text-[var(--dtu-accent)]" role="alert">{fbError}</p>}

          </section>
        )}

        <section aria-label="Saved events" className="mt-8 w-full rounded-3xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 md:p-8 shadow-xl">
          <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <p className="text-xs font-semibold tracking-[0.25em] text-[var(--text-subtle)] uppercase">Saved Events</p>
              <h3 className="mt-2 text-2xl font-bold text-[var(--text-primary)] md:text-3xl">Your liked events</h3>
              <p className="mt-2 max-w-2xl text-sm text-[var(--text-subtle)]">Events you like are saved here so you can quickly find them again.</p>
            </div>

            <div className="inline-flex items-center gap-2 rounded-full border border-[var(--panel-border)] bg-[var(--panel-bg)] px-4 py-2 text-sm font-semibold text-[var(--text-primary)]">
              <Heart size={16} fill="currentColor" />
              {likedEvents.length} saved
            </div>
          </div>

          {isLoadingLikedEvents ? (
            <p className="mt-6 text-sm text-[var(--text-subtle)]">Loading liked events...</p>
          ) : likedEvents.length === 0 ? (
            <div className="mt-6 rounded-2xl border border-dashed border-[var(--panel-border)] bg-[var(--input-bg)]/60 p-10 text-center">
              <Heart size={22} className="mx-auto text-[var(--text-subtle)]" />
              <p className="mt-3 text-base font-semibold text-[var(--text-primary)]">No liked events yet</p>
              <p className="mt-2 text-sm text-[var(--text-subtle)]">Tap the heart on an event to save it here.</p>
            </div>
          ) : (
            <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {likedEvents.map((event) => <SavedEventCard key={event.id} event={event} />)}
            </div>
          )}
        </section>
      </main>

      <Footer />
    </div>
  );
}
