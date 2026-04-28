import type { Event } from '../types';
import { NEW_EVENT_THRESHOLD_DAYS } from '../constants';
import { DEFAULT_EVENT_COVER_IMAGE_URL, formatEventStart, getEventCoverImageUrl } from '../utils/eventUtils';
import { FacebookLinkButton } from './FacebookLinkButton';
import { LikeButton } from './LikeButton';
import { ChevronDown } from 'lucide-react';
import { useRef, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

// small presentational card. Receives one event and renders a link + metadata.
export function EventCard({ event }: { event: Event }) {
  const [isExpanded, setIsExpanded] = useState(false);
  const navigate = useNavigate();

  // () => ("arrow function") means when "called, do this". Actually its a shorthand that
  // TS/JS has for defining functions; instead of writing "function toggleExpanded() { ... }",
  // we can write "const toggleExpanded = () => { ... }". The missing stuff is implied
  // and JS/TS understands it automatically (and will complain if it dont). Its a nice
  // piece of "syntactic sugar" (kinda like ? :).
  const toggleExpanded = () => setIsExpanded(!isExpanded);
  const handleCardClick = (eventId: string) => navigate(`/events/${eventId}`);

  const descriptionRef = useRef<HTMLDivElement>(null);
  const [hasMoreDescription, setHasMoreDescription] = useState(false);
  const coverImageUrl = getEventCoverImageUrl(event.coverImageUrl);

  // check if description has more content than shown in collapsed state (exceeds 3 lines)
  useEffect(() => {
    if (!descriptionRef.current || !event.description) {
      setHasMoreDescription(false);
      return;
    }
    // check if the content height exceeds the collapsed height (approximately 3 lines * line-height)
    // line-clamp-3 typically results in ~4.5rem height for text-sm
    const element = descriptionRef.current;
    setHasMoreDescription(element.scrollHeight > element.clientHeight);
  }, [event.description]);

  // detect "new" events:
  const isNew = (() => {
    // avoid `any` by widening the event type to include optional extra fields
    const e = event as Event & Partial<{ isNew: boolean; publishedAt: string; addedAt: string }>;
    if (typeof e.isNew === 'boolean') return e.isNew;
    const createdAt = e.createdAt ?? e.publishedAt ?? e.addedAt;
    if (createdAt) {
      const created = new Date(createdAt).getTime();
      return Number.isFinite(created) && (Date.now() - created) < NEW_EVENT_THRESHOLD_DAYS * 24 * 60 * 60 * 1000;
    }
    return false;
  })();

  return (
    // <div> = regular container. Removed <a> to prevent opening facebook link
    <div
      className="block focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--link-primary)] rounded-2xl cursor-pointer transition-all duration-300"

      // split chevron and card clicks
      onClick={() => { // () = > means when clicking the card
        if (!isExpanded) { // if not expanded, navigate to detail page
          handleCardClick(event.id);
        }
      }}
    >
      {/* card */}
      <div className="relative bubble flex flex-col overflow-visible">
        <LikeButton
          event={event}
          compact
          iconOnly
          className="absolute right-4 top-4 z-20"
        />

        {/* NEW badge */}
        {isNew && (
          <div className="mb-3">
            <span className="rounded-full bg-gradient-to-r from-[var(--dtu-accent)] to-[var(--dtu-accent-light)] text-white text-xs font-bold px-3 py-1.5 shadow-lg left-4 top-4 absolute z-10">
              New event
            </span>
          </div>
        )}

        {/* layout: image + text column */}
        <div className="flex flex-col gap-4 flex-1">
          <img
            src={coverImageUrl}
            alt={event.title}
            className="w-full h-48 object-cover rounded-xl shadow-md transition-transform duration-300"
            onError={(imageEvent) => {
              imageEvent.currentTarget.src = DEFAULT_EVENT_COVER_IMAGE_URL;
            }}
          />
          {/* text column */}
          <div className="min-w-0 flex-1 px-4">
            <div className="font-bold text-lg text-[var(--text-primary)] truncate">{event.title}</div>
            <div className="text-sm text-[var(--text-subtle)] mt-1">{formatEventStart(event.startTime)}</div>
            <div className="text-sm text-[var(--text-subtle)]">{event.place?.name ?? 'Location TBA'}</div>
            {/* ? = optional, ?? = if null/undefined then use 'Location TBA' */}
          </div>
        </div>

        {/* description section - preview when collapsed, full when expanded */}
        {/* if expanded and has description, show it with size h 32 (about 8 lines). Otherwise show 3 lines */}
        {event.description && (
          <div className="mt-4 px-4">
            <div
              ref={descriptionRef}
              className={`text-sm text-[var(--text-body)] overflow-hidden transition-all duration-300 ${
                isExpanded ? 'max-h-32 line-clamp-6' : 'line-clamp-3'
              }`}
            >
              {event.description}
            </div>
          </div>
        )}

        {/* bottom section: link button and chevron */}
        <div className="flex items-center justify-between mt-4 gap-2 px-4 pb-4">
          <FacebookLinkButton event={event} />

          {/* chevron button in bottom right - only show if description can be expanded */}
          {/* chevron means an arrow minus the stick so just the arrowhead */}
          {hasMoreDescription && (
            <button
              onClick={(e) => { // when clicking the chevron button...
                e.preventDefault();  // ...prevent the default link behavior
                e.stopPropagation(); // ...stop the click from setting off the card click
                toggleExpanded();    // ...do the actual expand/collapse
              }}
              className="bubble-button flex items-center gap-1 text-sm"
              aria-label={isExpanded ? 'Collapse event details' : 'Expand event details'}
            >
              <ChevronDown           // ...and rotate on expand
                className={`w-4 h-4 transition-transform ${isExpanded ? 'rotate-180' : ''}`}
              // ? : = "conditional operator": if isExpanded true then 'rotate-180' else nothing
              />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
