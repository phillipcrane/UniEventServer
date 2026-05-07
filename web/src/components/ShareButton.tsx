import { useState } from 'react';
import { Check, Share2 } from 'lucide-react';
import type { Event } from '../types';
import { getEventUrl } from '../utils/eventUtils';
import { SHARE_FEEDBACK_MS } from '../constants';

type ShareButtonProps = {
  event: Event;
  className?: string;
};

// share button with a fallback chain: native share sheet (mobile), then clipboard copy, then open in new tab.
export function ShareButton({ event, className = '' }: ShareButtonProps) {
  const [copied, setCopied] = useState(false);
  const handleCopied = () => { setCopied(true); window.setTimeout(() => setCopied(false), SHARE_FEEDBACK_MS); };
  const eventUrl = getEventUrl(event.id, event.eventURL);

  const handleShare = async (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    e.stopPropagation();

    const sharePayload = {
      title: event.title,
      text: 'Check out this event on UniEvent',
      url: eventUrl,
    };

    try {
      if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
        // navigator.share opens the native OS share sheet (works on mobile + some desktop browsers)
        await navigator.share(sharePayload);
        return;
      }

      if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
        // native share not available, fall back to copying the link to clipboard
        await navigator.clipboard.writeText(eventUrl);
        handleCopied();
        return;
      }

      // last resort: just open the link directly in a new tab
      window.open(eventUrl, '_blank', 'noopener,noreferrer');
    } catch {
      // Ignore aborted shares and clipboard errors to avoid noisy UX.
    }
  };

  return (
    <button
      type="button"
      onClick={handleShare}
      className={`inline-flex items-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-4 py-2 text-sm font-semibold text-[var(--text-primary)] transition-all duration-200 hover:-translate-y-0.5 hover:bg-[var(--button-hover)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--input-focus-border)] ${className}`.trim()}
      aria-label="Share event"
      title={copied ? 'Link copied' : 'Share event'}
    >
      {copied ? <Check size={16} /> : <Share2 size={16} />}
      {copied ? 'Copied' : 'Share'}
    </button>
  );
}
