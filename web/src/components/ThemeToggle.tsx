import { useState, useEffect } from 'react';
import { Sun, Moon } from 'lucide-react';

const THEME_STORAGE_KEY = 'ui-theme';

export function ThemeToggle() {
  const [dark, setDark] = useState<boolean>(() => {
    if (typeof window === 'undefined') {
      return true;
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
    <label className="relative inline-flex h-8 w-[60px] cursor-pointer items-center max-[640px]:h-7 max-[640px]:w-12">
      <input
        type="checkbox"
        checked={dark}
        onChange={() => setDark(d => !d)}
        aria-label="Toggle dark mode"
        className="peer sr-only"
      />
      <span className="absolute inset-0 rounded-full border border-zinc-300 bg-zinc-300 transition-colors duration-300 peer-checked:border-black peer-checked:bg-black dark:border-zinc-500 dark:bg-zinc-700 dark:peer-checked:border-zinc-900 dark:peer-checked:bg-zinc-900" />
      <span className="absolute left-[3px] top-[3px] flex h-[26px] w-[26px] items-center justify-center rounded-full bg-white text-zinc-900 shadow-[0_2px_4px_rgba(0,0,0,0.25)] transition-transform duration-300 peer-checked:translate-x-7 peer-checked:bg-zinc-900 peer-checked:text-white max-[640px]:h-[22px] max-[640px]:w-[22px] max-[640px]:peer-checked:translate-x-5">
          {dark ? <Moon size={18} /> : <Sun size={18} />}
      </span>
    </label>
  );
}