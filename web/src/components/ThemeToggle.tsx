import { useState, useEffect } from 'react';
import { Sun, Moon } from 'lucide-react';
import '../styles/ThemeToggle.css';

const THEME_STORAGE_KEY = 'ui-theme';

// toggle between dark and light mode. persists the choice to localStorage so it survives a page reload.
export function ThemeToggle() {
  // useState with a function ("lazy initializer") runs it once on first render to set the initial value.
  // used here so we read localStorage once at startup rather than on every re-render.
  const [dark, setDark] = useState<boolean>(() => {
    if (typeof window === 'undefined') {
      return true; // server-side rendering guard, default to dark
    }

    try {
      const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
      if (storedTheme === 'dark') {
        return true;
      }
      if (storedTheme === 'light') {
        return false;
      }
    } catch {
      // Ignore storage access issues and fall back to default theme.
    }

    return true;
  });

  useEffect(() => {
    // document.documentElement is the <html> element. toggling "dark" on it switches all the
    // CSS variables defined in index.css between their dark and light values.
    const root = document.documentElement;
    if (dark) root.classList.add('dark');
    else root.classList.remove('dark');

    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, dark ? 'dark' : 'light');
    } catch {
      // Ignore storage access issues and keep runtime theme in memory.
    }
  }, [dark]);

  return (
    <label className="theme-slider">
      <input
        type="checkbox"
        checked={dark}
        onChange={() => setDark(d => !d)}
        aria-label="Toggle dark mode"
      />
      <span className="track">
        <span className="thumb">
          {dark ? <Moon size={18} /> : <Sun size={18} />}
        </span>
      </span>
    </label>
  );
}