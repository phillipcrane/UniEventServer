import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { ThemeToggle } from '../components/ThemeToggle';
import { Footer } from '../components/Footer';
import { getAccountProfile, signOutCurrentUser, type AccountRole, type AuthUser } from '../services/auth';
import { useAuth } from '../context/AuthContext';
import { buildFacebookLoginUrl } from '../services/facebook';
import { getEvents } from '../services/dal';
import { formatEventStart } from '../utils/eventUtils';
import type { Event as EventType } from '../types';
import { LikeButton } from '../components/LikeButton';
import { useLikes } from '../context/LikesContext';
import { CalendarDays, CircleUserRound, Heart, LogOut, MapPin, Ticket } from 'lucide-react';

function buildUsername(user: AuthUser | null) {
    if (!user) return 'username';

    const emailLocalPart = user.email?.split('@')[0]?.trim();
    if (emailLocalPart) return emailLocalPart;

    const displayName = user.displayName?.trim();
    if (displayName) return displayName.toLowerCase().replace(/\s+/g, '.');

    return 'username';
}

function filterAndSortLikedEvents(events: EventType[], likedEventIds: string[]) {
    const likedEventIdSet = new Set(likedEventIds);

    return events
        .filter((event) => likedEventIdSet.has(event.id))
        .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
}

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
                    <div className="flex h-full w-full items-center justify-center bg-[linear-gradient(135deg,rgba(59,130,246,0.35),rgba(20,184,166,0.25))]">
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
    const navigate = useNavigate();
    const { currentUser } = useAuth();
    const { likedIds } = useLikes();
    const [accountRole, setAccountRole] = useState<AccountRole>(currentUser?.role ?? 'user');
    const [organizerNames, setOrganizerNames] = useState<string[]>([]);
    const [isSigningOut, setIsSigningOut] = useState(false);
    const [allEvents, setAllEvents] = useState<EventType[]>([]);
    const [isLoadingLikedEvents, setIsLoadingLikedEvents] = useState(true);

    useEffect(() => {
        if (currentUser === null) {
            navigate('/login', { replace: true });
        }
    }, [currentUser, navigate]);

    async function handleSignOut() {
        const shouldSignOut = window.confirm('Are you sure you want to log out?');
        if (!shouldSignOut) {
            return;
        }

        try {
            setIsSigningOut(true);
            await signOutCurrentUser();
            navigate('/login', { replace: true });
        } finally {
            setIsSigningOut(false);
        }
    }

    const userLabel = currentUser?.displayName || currentUser?.email || 'Profile';
    const username = buildUsername(currentUser);
    const profileImage = currentUser?.photoURL;

    useEffect(() => {
        setAccountRole(currentUser?.role ?? 'user');
        setOrganizerNames(Array.isArray(currentUser?.organizerNames) ? currentUser.organizerNames : []);
    }, [currentUser?.role, currentUser?.organizerNames]);

    useEffect(() => {
        let cancelled = false;

        const loadAccountProfile = async () => {
            try {
                const profile = await getAccountProfile(currentUser?.uid);
                if (cancelled) {
                    return;
                }

                setAccountRole(profile.role);
                setOrganizerNames(profile.organizerNames);
            } catch {
                if (cancelled) {
                    return;
                }

                setAccountRole(currentUser?.role ?? 'user');
                setOrganizerNames(Array.isArray(currentUser?.organizerNames) ? currentUser.organizerNames : []);
            }
        };

        void loadAccountProfile();

        return () => {
            cancelled = true;
        };
    }, [currentUser?.uid, currentUser?.role, currentUser?.organizerNames]);

    useEffect(() => {
        let cancelled = false;

        const loadEvents = async () => {
            if (!currentUser?.uid) {
                setAllEvents([]);
                setIsLoadingLikedEvents(false);
                return;
            }

            setIsLoadingLikedEvents(true);

            try {
                const events = await getEvents();

                if (cancelled) {
                    return;
                }

                setAllEvents(events);
            } finally {
                if (!cancelled) {
                    setIsLoadingLikedEvents(false);
                }
            }
        };

        void loadEvents();

        return () => {
            cancelled = true;
        };
    }, [currentUser?.uid]);

    const likedEvents = useMemo(() => {
        return filterAndSortLikedEvents(allEvents, Array.from(likedIds));
    }, [allEvents, likedIds]);

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
                    <div className="grid grid-cols-1 gap-6 lg:grid-cols-4 lg:items-start">
                        <div className="lg:col-span-3">
                            <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-start">
                                <div className="relative flex h-36 w-36 flex-shrink-0 items-center justify-center overflow-hidden rounded-full border-4 border-[var(--dtu-accent-light)] bg-[#0f1020] shadow-[0_0_0_8px_rgba(60,84,240,0.14)]">
                                    {profileImage ? (
                                        <img src={profileImage} alt={userLabel} className="h-full w-full object-cover" />
                                    ) : (
                                        <CircleUserRound aria-label="Default profile picture" className="h-[86%] w-[86%] text-white" strokeWidth={1.55} />
                                    )}
                                </div>

                                <div className="flex-1 space-y-4 text-center sm:text-left">
                                    <div>
                                        <h2 className="text-3xl font-bold text-[var(--text-primary)]">{username}</h2>
                                        <span className={`mt-2 inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.1em] ${accountRole === 'organizer'
                                            ? 'border border-transparent bg-[var(--link-primary)] text-white'
                                            : 'border border-[var(--panel-border)] bg-[var(--panel-bg)] text-[var(--text-primary)]'
                                            }`}>
                                            {accountRole === 'organizer' ? 'Organizer' : 'User'}
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
                                    </div>
                                </div>
                            </div>
                        </div>

                        {accountRole === 'organizer' && (
                            <aside className="rounded-xl border border-[var(--panel-border)] bg-[color-mix(in_srgb,var(--panel-bg)_72%,var(--input-bg)_28%)] p-4 shadow-sm">
                                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[var(--text-subtle)]">Organizations</p>
                                <div className="mt-3 space-y-2">
                                    {organizerNames.length ? organizerNames.map((organization) => (
                                        <div key={organization} className="inline-flex w-full items-center justify-center rounded-full border border-[var(--panel-border)] bg-[var(--panel-bg)]/85 px-3 py-2 text-xs font-semibold text-[var(--text-primary)]">
                                            {organization}
                                        </div>
                                    )) : (
                                        <p className="py-3 text-xs text-[var(--text-subtle)]">No organizations linked yet.</p>
                                    )}
                                </div>
                            </aside>
                        )}
                    </div>
                </section>

                {accountRole === 'organizer' && (
                    <section className="mt-6 rounded-2xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 shadow-lg">
                        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[var(--text-subtle)]">
                                    Facebook Integration
                                </p>
                                <h3 className="mt-1 text-lg font-bold text-[var(--text-primary)]">
                                    Connect your Facebook Page
                                </h3>
                            </div>

                            <a
                                href={buildFacebookLoginUrl()}
                                className="inline-flex items-center justify-center rounded-lg bg-[var(--link-primary)] px-6 py-3 text-sm font-semibold text-white transition-colors duration-200 hover:bg-[var(--link-primary-hover)]"
                            >
                                Connect Facebook Page
                            </a>
                        </div>

                        <div className="mt-4 rounded-xl border border-[var(--panel-border)] bg-[var(--input-bg)]/65 p-4">
                            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[var(--text-subtle)]">
                                Manual Event
                            </p>
                            <h4 className="mt-1 text-base font-bold text-[var(--text-primary)]">
                                Add event manually
                            </h4>
                            <p className="mt-1 text-sm text-[var(--text-subtle)]">
                                Create and review event details in a dedicated organizer form.
                            </p>
                            <Link
                                to="/organizer/events/new"
                                className="mt-3 inline-flex items-center justify-center rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-4 py-2.5 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)]"
                            >
                                Open Manual Event Form
                            </Link>
                        </div>
                    </section>
                )}

                <section aria-label="Saved events" className="mt-8 w-full rounded-3xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 md:p-8 shadow-xl">
                    <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
                        <div>
                            <p className="text-xs font-semibold tracking-[0.25em] text-[var(--text-subtle)] uppercase">
                                Saved Events
                            </p>
                            <h3 className="mt-2 text-2xl font-bold text-[var(--text-primary)] md:text-3xl">
                                Your liked events
                            </h3>
                            <p className="mt-2 max-w-2xl text-sm text-[var(--text-subtle)]">
                                Events you like are saved here so you can quickly find them again.
                            </p>
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
                            <p className="mt-3 text-base font-semibold text-[var(--text-primary)]">
                                No liked events yet
                            </p>
                            <p className="mt-2 text-sm text-[var(--text-subtle)]">
                                Tap the heart on an event to save it here.
                            </p>
                        </div>
                    ) : (
                        <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                            {likedEvents.map((event) => (
                                <SavedEventCard key={event.id} event={event} />
                            ))}
                        </div>
                    )}
                </section>
            </main>

            <Footer />
        </div>
    );
}