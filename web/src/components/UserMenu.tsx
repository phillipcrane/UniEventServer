import { useCallback, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { CircleUserRound, LogOut } from 'lucide-react';
import { useClickOutside } from '../hooks/useClickOutside';

type UserMenuProps = {
  userLabel: string;
  onSignOut: () => void;
  isSigningOut: boolean;
};

// dropdown account menu in the header. shows who's signed in and offers profile + log out.
export function UserMenu({ userLabel, onSignOut, isSigningOut }: UserMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);
  // useCallback keeps the same function reference across renders so useClickOutside doesnt re-subscribe on every render.
  const handleClose = useCallback(() => setIsOpen(false), []);
  useClickOutside(menuRef, isOpen, handleClose); // close the menu when clicking outside it

  return (
    <div className="relative" ref={menuRef}>
      <button
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        className="inline-flex items-center justify-center rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--input-focus-border)]"
        aria-label="Open account menu"
        aria-expanded={isOpen} // tells screen readers whether the dropdown is currently open
      >
        <CircleUserRound size={18} />
      </button>

      {/* dropdown panel - only rendered while isOpen is true */}
      {isOpen && (
        <div className="absolute right-0 z-20 mt-2 w-64 rounded-xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-2 shadow-xl">
          <p className="px-2 py-2 text-xs font-semibold text-[var(--text-subtle)]">
            Signed in as
          </p>
          <p className="truncate px-2 pb-2 text-sm font-semibold text-[var(--text-primary)]">
            {userLabel}
          </p>
          <Link
            to="/profile"
            onClick={() => setIsOpen(false)}
            className="mt-1 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)]"
          >
            <CircleUserRound size={16} />
            Profile
          </Link>
          <button
            type="button"
            onClick={onSignOut}
            disabled={isSigningOut}
            className="mt-2 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)] disabled:cursor-not-allowed disabled:opacity-70"
          >
            <LogOut size={16} />
            {isSigningOut ? 'Signing out...' : 'Log out'}
          </button>
        </div>
      )}
    </div>
  );
}
