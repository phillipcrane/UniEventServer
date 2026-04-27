import { useMemo } from 'react';
import type { Page, SortMode } from '../types';
import { MultiSelectFilter } from './MultiSelectFilter';
import { CalendarRange, Search, SlidersHorizontal, X } from 'lucide-react';
import { toInputDateString, addDays, addMonths } from '../utils/dateUtils';

// render a multi-select dropdown for pages
function PageFilter({ pages, pageIds, setPageIds }: { pages: Page[]; pageIds: string[]; setPageIds: (v: string[]) => void }) {
  return (
    <div className="flex flex-col gap-2">
      <label className="text-xs font-semibold uppercase tracking-widest text-[var(--text-subtle)]">Organizer</label>
      <MultiSelectFilter
        pages={pages}
        selectedIds={pageIds}
        onSelectionChange={setPageIds}
      />
    </div>
  );
}

// render a search box with event count
function SearchBox({ query, setQuery, count }: { query: string; setQuery: (v: string) => void; count: number }) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor="q" className="text-xs font-semibold uppercase tracking-widest text-[var(--text-subtle)]">Search</label>
      <div className="flex items-center gap-2">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]" />
          <input
            id="q"
            type="text"
            placeholder="Search title, description, venue..."
            className="w-full rounded-lg border-2 bg-[var(--input-bg)] border-[var(--input-border)] py-2.5 pl-9 pr-9 text-[var(--input-text)] focus:border-[var(--input-focus-border)] focus:outline-none focus:ring-3 focus:ring-[var(--button-hover)] transition-all duration-200"
            value={query}
            onChange={e => setQuery(e.target.value)}
          />
          {query && (
            <button
              type="button"
              onClick={() => setQuery('')}
              aria-label="Clear search"
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-[var(--text-subtle)] transition hover:bg-[var(--button-hover)] hover:text-[var(--text-primary)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--input-focus-border)]"
            >
              <X size={14} />
            </button>
          )}
        </div>
        <input
          readOnly
          aria-label="Matching events"
          value={`${count} result${count === 1 ? '' : 's'}`}
          className="w-28 rounded-lg border border-[var(--panel-border)] bg-[var(--button-hover)] px-3 py-2.5 text-center text-xs font-semibold text-[var(--text-subtle)]"
        />
      </div>
    </div>
  );
}

// date range with quick presets + custom dates
function DateRangeFilter({ fromDate, setFromDate, toDate, setToDate }: { fromDate: string; setFromDate: (v: string) => void; toDate: string; setToDate: (v: string) => void }) {
  const applyPreset = (preset: '7d' | '30d' | '3m') => {
    const start = new Date();
    const end = preset === '7d'
      ? addDays(start, 7)
      : preset === '30d'
        ? addDays(start, 30)
        : addMonths(start, 3);

    setFromDate(toInputDateString(start));
    setToDate(toInputDateString(end));
  };

  const activePreset = useMemo((): '7d' | '30d' | '3m' | null => {
    const today = toInputDateString(new Date());
    if (fromDate !== today) return null;
    if (toDate === toInputDateString(addDays(new Date(), 7))) return '7d';
    if (toDate === toInputDateString(addDays(new Date(), 30))) return '30d';
    if (toDate === toInputDateString(addMonths(new Date(), 3))) return '3m';
    return null;
  }, [fromDate, toDate]);

  return (
    <div className="flex min-w-0 flex-col gap-2">
      <label className="text-xs font-semibold uppercase tracking-widest text-[var(--text-subtle)]">Date Range</label>
      <div className="date-preset-row flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => applyPreset('7d')}
          className={`rounded-full border px-3 py-1 text-xs font-semibold transition ${activePreset === '7d'
            ? 'border-transparent bg-[var(--link-primary)] text-white'
            : 'border-[var(--panel-border)] bg-[var(--panel-bg)] text-[var(--text-primary)] hover:bg-[var(--button-hover)]'
            }`}
        >
          Next 7 days
        </button>
        <button
          type="button"
          onClick={() => applyPreset('30d')}
          className={`rounded-full border px-3 py-1 text-xs font-semibold transition ${activePreset === '30d'
            ? 'border-transparent bg-[var(--link-primary)] text-white'
            : 'border-[var(--panel-border)] bg-[var(--panel-bg)] text-[var(--text-primary)] hover:bg-[var(--button-hover)]'
            }`}
        >
          Next 30 days
        </button>
        <button
          type="button"
          onClick={() => applyPreset('3m')}
          className={`rounded-full border px-3 py-1 text-xs font-semibold transition ${activePreset === '3m'
            ? 'border-transparent bg-[var(--link-primary)] text-white'
            : 'border-[var(--panel-border)] bg-[var(--panel-bg)] text-[var(--text-primary)] hover:bg-[var(--button-hover)]'
            }`}
        >
          Next 3 months
        </button>
      </div>
      <div className="date-range-row flex w-full min-w-0 items-center gap-1.5 rounded-xl border border-[var(--input-border)] bg-[var(--input-bg)] px-2 py-1.5 sm:gap-2 sm:px-3">
        <CalendarRange size={16} className="shrink-0 text-[var(--text-subtle)]" aria-hidden="true" />
        <input
          type="date"
          className="date-range-input w-0 min-w-0 flex-1 rounded-lg border border-transparent bg-transparent px-2 py-2 text-sm text-[var(--input-text)] focus:border-[var(--input-focus-border)] focus:outline-none focus:ring-2 focus:ring-[var(--button-hover)] sm:text-base"
          value={fromDate}
          onChange={e => setFromDate(e.target.value)}
          title="Start date"
          aria-label="Start date"
        />
        <span className="date-range-separator self-center px-0.5 text-[var(--text-subtle)] font-medium sm:px-2">to</span>
        <input
          type="date"
          className="date-range-input w-0 min-w-0 flex-1 rounded-lg border border-transparent bg-transparent px-2 py-2 text-sm text-[var(--input-text)] focus:border-[var(--input-focus-border)] focus:outline-none focus:ring-2 focus:ring-[var(--button-hover)] sm:text-base"
          value={toDate}
          onChange={e => setToDate(e.target.value)}
          title="End date"
          aria-label="End date"
        />
        {(fromDate || toDate) && (
          <button
            type="button"
            onClick={() => {
              setFromDate('');
              setToDate('');
            }}
            aria-label="Clear date range"
            className="rounded-md p-1 text-[var(--text-subtle)] transition hover:bg-[var(--button-hover)] hover:text-[var(--text-primary)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--input-focus-border)]"
          >
            <X size={14} />
          </button>
        )}
      </div>
    </div>
  );
}

// upcoming/newest filter
function SortFilter({ sortMode, setSortMode }: { sortMode: SortMode; setSortMode: (v: SortMode) => void }) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor="sort" className="text-xs font-semibold uppercase tracking-widest text-[var(--text-subtle)]">Sort by</label>
      <select
        id="sort"
        className="w-full rounded-lg border-2 bg-[var(--input-bg)] border-[var(--input-border)] px-4 py-2.5 text-[var(--input-text)] focus:border-[var(--input-focus-border)] focus:outline-none focus:ring-3 focus:ring-[var(--button-hover)] transition-all duration-200 cursor-pointer"
        value={sortMode}
        onChange={e => setSortMode(e.target.value as SortMode)}
      >
        <option value="upcoming">Upcoming</option>
        <option value="newest">Recently added</option>
        <option value="all">All events</option>
      </select>
    </div>
  );
}

function ActiveFilters(props: {
  pages: Page[];
  pageIds: string[];
  setPageIds: (v: string[]) => void;
  query: string;
  setQuery: (v: string) => void;
  fromDate: string;
  setFromDate: (v: string) => void;
  toDate: string;
  setToDate: (v: string) => void;
  sortMode: SortMode;
  setSortMode: (v: SortMode) => void;
}) {
  const activePageFilters = props.pages.filter((page) => props.pageIds.includes(page.id));
  const hasActiveFilters =
    !!props.query ||
    props.pageIds.length > 0 ||
    !!props.fromDate ||
    !!props.toDate ||
    props.sortMode !== 'upcoming';

  if (!hasActiveFilters) {
    return null;
  }

  return (
    <div className="mt-3 flex flex-wrap items-center gap-2 pt-4">
      <span className="text-xs font-semibold uppercase tracking-widest text-[var(--text-subtle)]">Active filters</span>

      {props.query && (
        <button
          type="button"
          onClick={() => props.setQuery('')}
          className="inline-flex items-center gap-1 rounded-full border border-[var(--panel-border)] bg-[var(--button-hover)] px-3 py-1 text-xs font-semibold text-[var(--text-primary)] transition hover:border-[var(--input-focus-border)]"
        >
          Search: "{props.query}"
          <X size={12} />
        </button>
      )}

      {activePageFilters.map((page) => (
        <button
          key={page.id}
          type="button"
          onClick={() => props.setPageIds(props.pageIds.filter((id) => id !== page.id))}
          className="inline-flex items-center gap-1 rounded-full border border-[var(--panel-border)] bg-[var(--button-hover)] px-3 py-1 text-xs font-semibold text-[var(--text-primary)] transition hover:border-[var(--input-focus-border)]"
        >
          Organizer: {page.name}
          <X size={12} />
        </button>
      ))}

      {(props.fromDate || props.toDate) && (
        <button
          type="button"
          onClick={() => {
            props.setFromDate('');
            props.setToDate('');
          }}
          className="inline-flex items-center gap-1 rounded-full border border-[var(--panel-border)] bg-[var(--button-hover)] px-3 py-1 text-xs font-semibold text-[var(--text-primary)] transition hover:border-[var(--input-focus-border)]"
        >
          Date: {props.fromDate || 'Any'} to {props.toDate || 'Any'}
          <X size={12} />
        </button>
      )}

      {props.sortMode !== 'upcoming' && (
        <button
          type="button"
          onClick={() => props.setSortMode('upcoming')}
          className="inline-flex items-center gap-1 rounded-full border border-[var(--panel-border)] bg-[var(--button-hover)] px-3 py-1 text-xs font-semibold text-[var(--text-primary)] transition hover:border-[var(--input-focus-border)]"
        >
          Sort: {props.sortMode === 'newest' ? 'Recently added' : 'All events'}
          <X size={12} />
        </button>
      )}

      <button
        type="button"
        onClick={() => {
          props.setQuery('');
          props.setPageIds([]);
          props.setFromDate('');
          props.setToDate('');
          props.setSortMode('upcoming');
        }}
        className="ml-auto inline-flex items-center gap-1 rounded-full border border-[var(--dtu-accent)] bg-transparent px-3 py-1 text-xs font-semibold text-[var(--dtu-accent)] transition hover:bg-[var(--button-hover)]"
      >
        Clear all
      </button>
    </div>
  );
}

// main component combining the filters
export function FilterBar(props: {
  pages: Page[]; // pages to show
  pageIds: string[]; // currently selected organizer ids
  setPageIds: (v: string[]) => void;
  query: string;
  setQuery: (v: string) => void;
  fromDate: string;
  setFromDate: (v: string) => void;
  toDate: string;
  setToDate: (v: string) => void;
  count: number;
  sortMode: SortMode;
  setSortMode: (v: SortMode) => void;
}) {
  return (
    <div className="space-y-6">
      <section
        aria-label="Event filters"
        className="backdrop-filter backdrop-blur-md bg-[var(--panel-bg)] border border-[var(--panel-border)] rounded-2xl p-6 shadow-lg transition-all duration-300"
      >
        <div className="mb-4 flex items-center justify-between gap-2">
          <div className="inline-flex items-center gap-2 text-[var(--text-primary)]">
            <SlidersHorizontal size={16} />
            <h2 className="text-sm font-semibold uppercase tracking-widest">Filter events</h2>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6 mb-4">
          <div className="md:col-span-1 lg:col-span-1">
            <PageFilter pages={props.pages} pageIds={props.pageIds} setPageIds={props.setPageIds} />
          </div>
          <div className="md:col-span-2 lg:col-span-2">
            <SearchBox query={props.query} setQuery={props.setQuery} count={props.count} />
          </div>
          <div className="md:col-span-1 lg:col-span-1">
            <SortFilter sortMode={props.sortMode} setSortMode={props.setSortMode} />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-2 gap-6">
          <div className="md:col-span-3 lg:col-span-1">
            <DateRangeFilter
              fromDate={props.fromDate}
              setFromDate={props.setFromDate}
              toDate={props.toDate}
              setToDate={props.setToDate}
            />
          </div>
        </div>

        <ActiveFilters
          pages={props.pages}
          pageIds={props.pageIds}
          setPageIds={props.setPageIds}
          query={props.query}
          setQuery={props.setQuery}
          fromDate={props.fromDate}
          setFromDate={props.setFromDate}
          toDate={props.toDate}
          setToDate={props.setToDate}
          sortMode={props.sortMode}
          setSortMode={props.setSortMode}
        />
      </section>
    </div>
  );
}



