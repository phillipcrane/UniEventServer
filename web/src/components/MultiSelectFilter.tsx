import { useState, useRef } from 'react';
import type { Page } from '../types';
import { useClickOutside } from '../hooks/useClickOutside';

// dropdown that lets users pick multiple organizers. shows selected ones as removable tags above a checkbox list.
export function MultiSelectFilter({
  pages,
  selectedIds,
  onSelectionChange,
}: {
  pages: Page[];
  selectedIds: string[];
  onSelectionChange: (ids: string[]) => void;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useClickOutside(containerRef, isOpen, () => setIsOpen(false)); // close dropdown when clicking anywhere outside it

  const handleToggle = (pageId: string) => {
    // if already selected, remove it. if not, add it. [...array, item] spreads existing items and appends the new one.
    const updated = selectedIds.includes(pageId)
      ? selectedIds.filter(id => id !== pageId)
      : [...selectedIds, pageId];
    onSelectionChange(updated);
  };

  const handleRemoveTag = (pageId: string, e: React.MouseEvent) => {
    e.stopPropagation(); // stop the click from bubbling up to the parent div and toggling the dropdown
    handleToggle(pageId);
  };

  const selectedPages = pages.filter(p => selectedIds.includes(p.id));

  return (
    <div ref={containerRef} className="relative w-full">
      {/* selected-tags display area. role="button" makes it keyboard-accessible,
           and the onKeyDown below lets users open/close it with Enter or Space */}
      <div
        role="button"
        tabIndex={0}
        aria-label="Toggle organizer filter"
        onClick={() => setIsOpen(!isOpen)}
        onKeyDown={(event) => {
          if (event.key !== 'Enter' && event.key !== ' ') {
            return; // only respond to Enter and Space, like a native button would
          }
          event.preventDefault();
          setIsOpen(!isOpen);
        }}
        className="w-full flex items-center flex-wrap gap-2 px-4 py-2.5 bg-[var(--input-bg)] border-2 border-[var(--input-border)] text-[var(--input-text)] rounded-lg focus:outline-none focus:border-[var(--input-focus-border)] focus:ring-3 focus:ring-[var(--button-hover)] min-h-[44px] hover:border-[var(--dtu-accent)] transition-all duration-200 cursor-pointer"
      >
        {selectedPages.length > 0 ? (
          selectedPages.map(page => (
            <span
              key={page.id}
              className="inline-flex items-center gap-1.5 bg-[var(--button-hover)] text-[var(--link-primary)] px-3 py-1.5 rounded-lg text-xs font-semibold border border-[var(--link-primary)] transition-all duration-200"
            >
              {page.name}
              <button
                type="button"
                aria-label={`Remove ${page.name}`}
                onClick={e => handleRemoveTag(page.id, e)}
                className="ml-0.5 text-[var(--link-primary)] hover:text-[var(--link-primary-hover)] font-bold text-sm leading-none"
              >
                ×
              </button>
            </span>
          ))
        ) : (
          <span className="text-[var(--text-subtle)] text-sm">Select organizers...</span>
        )}
      </div>

      {/* Dropdown Menu */}
      {isOpen && (
        <div className="absolute z-50 w-full mt-2 bg-[var(--panel-bg)] border border-[var(--panel-border)] rounded-lg shadow-lg backdrop-filter backdrop-blur-md transition-all duration-200">
          {pages.length === 0 ? (
            <div className="p-4 text-sm text-[var(--text-subtle)]">No organizers available</div>
          ) : (
            <div className="max-h-64 overflow-y-auto">
              {pages.map(page => (
                <label
                  key={page.id}
                  className="flex items-center gap-3 px-4 py-3 hover:bg-[var(--button-hover)] cursor-pointer transition-all duration-150 text-[var(--text-primary)]"
                >
                  <input
                    type="checkbox"
                    checked={selectedIds.includes(page.id)}
                    onChange={() => handleToggle(page.id)}
                    className="w-4 h-4 accent-[var(--link-primary)] rounded cursor-pointer"
                  />
                  <span className="flex-1 text-sm font-medium">{page.name}</span>
                </label>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
