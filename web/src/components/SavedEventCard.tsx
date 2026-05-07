import { Link } from 'react-router-dom';
import { CalendarDays, Heart, MapPin, Ticket } from 'lucide-react';
import { LikeButton } from './LikeButton';
import { formatEventStart } from '../utils/eventUtils';
import type { Event } from '../types';

// card variant for saved events, shown on the profile page.
export function SavedEventCard({ event }: { event: Event }) {
  return (
    // "group" is a Tailwind feature: child elements can react to the parent being hovered
    // using "group-hover:" prefixed classes (see the image's group-hover:scale-105 below).
    <article className="group overflow-hidden rounded-2xl border border-[var(--panel-border)] bg-[var(--input-bg)]/75 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl focus-within:ring-2 focus-within:ring-[var(--input-focus-border)]">
      {/* image section - clicking goes to the event detail page */}
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

      {/* text section */}
      <div className="space-y-3 p-4">
        {/* saved badge + unlike button */}
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

        {/* metadata rows: date, location, saved status */}
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
