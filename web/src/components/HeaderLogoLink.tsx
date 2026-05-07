import { Link, useLocation } from 'react-router-dom';

const LOGO_SRC = '/dtuevent-logo.png';

// logo in the header. navigates home normally, but if you're already on the home page it does
// a full reload instead so the event list refreshes.
export function HeaderLogoLink() {
  const location = useLocation();
  const isHomePage = location.pathname === '/';
  const tooltip = isHomePage ? 'Home (refresh page)' : 'Go to home';

  return (
    <Link
      to="/"
      reloadDocument={isHomePage} // reloadDocument forces a full page reload instead of client-side navigation
      aria-label={isHomePage ? 'Refresh the main page' : 'Go to the main page'}
      title={tooltip}
      className="header-logo-link group relative"
    >
      <img src={LOGO_SRC} alt="UniEvent Logo" className="header-logo" />
      {/* tooltip that appears on hover. aria-hidden hides it from screen readers since the Link's aria-label already covers it */}
      <span
        aria-hidden="true"
        className="pointer-events-none absolute -bottom-8 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-md border border-[var(--panel-border)] bg-[var(--panel-bg)] px-2 py-1 text-[10px] font-semibold uppercase tracking-wide text-[var(--text-subtle)] opacity-0 shadow-md transition-opacity duration-200 group-hover:opacity-100 group-focus-visible:opacity-100"
      >
        {tooltip}
      </span>
    </Link>
  );
}