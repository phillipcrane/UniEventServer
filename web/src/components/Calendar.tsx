import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Event } from '../types';
import { formatEventStart } from '../utils/eventUtils';

// consts are usually kept in constants.ts, but this is highly local.
const WEEK_DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

function formatYMD(d: Date) {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function startOfWeek(date: Date) {
  // Return Monday for the week of the given date
  const day = (date.getDay() + 6) % 7; // Monday=0 .. Sunday=6
  const result = new Date(date);
  result.setHours(0, 0, 0, 0);
  result.setDate(result.getDate() - day);
  return result;
}

function buildMonthGrid(viewDate: Date) {
  const firstOfMonth = new Date(viewDate.getFullYear(), viewDate.getMonth(), 1);
  const startWeekDay = (firstOfMonth.getDay() + 6) % 7; // shift so Monday=0

  const grid: Date[][] = [];
  const startDate = new Date(firstOfMonth);
  startDate.setDate(startDate.getDate() - startWeekDay);

  for (let week = 0; week < 6; week += 1) {
    const row: Date[] = [];
    for (let day = 0; day < 7; day += 1) {
      row.push(new Date(startDate));
      startDate.setDate(startDate.getDate() + 1);
    }
    grid.push(row);
  }

  return grid;
}

function buildWeekGrid(viewDate: Date) {
  const start = startOfWeek(viewDate);
  const row: Date[] = [];
  for (let i = 0; i < 7; i += 1) {
    const d = new Date(start);
    d.setDate(start.getDate() + i);
    row.push(d);
  }
  return [row];
}

export function CalendarView({ events }: { events: Event[] }) {
  const navigate = useNavigate();
  const [viewDate, setViewDate] = useState(() => new Date());
  const [viewMode, setViewMode] = useState<'month' | 'week'>('month');

  const grid = useMemo(() => {
    return viewMode === 'month' ? buildMonthGrid(viewDate) : buildWeekGrid(viewDate);
  }, [viewDate, viewMode]);

  const eventsByDate = useMemo(() => {
    const map = new Map<string, Event[]>();

    const addEventForKey = (key: string, evt: Event) => {
      const existing = map.get(key) ?? [];
      existing.push(evt);
      map.set(key, existing);
    };

    for (const evt of events) {
      const start = new Date(evt.startTime);
      if (Number.isNaN(start.getTime())) continue;

      const end = evt.endTime ? new Date(evt.endTime) : undefined;
      const endDate = end && !Number.isNaN(end.getTime()) ? end : start;

      const startDay = new Date(start);
      startDay.setHours(0, 0, 0, 0);

      const endDay = new Date(endDate);
      endDay.setHours(0, 0, 0, 0);

      // Avoid unbounded looping on invalid end dates
      const maxDays = 30;
      const cursor = new Date(startDay);
      let days = 0;

      while (cursor <= endDay && days < maxDays) {
        addEventForKey(formatYMD(cursor), evt);
        cursor.setDate(cursor.getDate() + 1);
        days += 1;
      }

      // If event spans more than maxDays, ensure at least start and end are represented
      if (days >= maxDays && endDay >= cursor) {
        addEventForKey(formatYMD(endDay), evt);
      }
    }
    return map;
  }, [events]);

  const todayKey = formatYMD(new Date());
  const currentMonth = viewDate.getMonth();
  const currentYear = viewDate.getFullYear();

  const goPrev = () => {
    setViewDate(d => {
      if (viewMode === 'month') {
        return new Date(d.getFullYear(), d.getMonth() - 1, 1);
      }
      const next = new Date(d);
      next.setDate(next.getDate() - 7);
      return next;
    });
  };

  const goNext = () => {
    setViewDate(d => {
      if (viewMode === 'month') {
        return new Date(d.getFullYear(), d.getMonth() + 1, 1);
      }
      const next = new Date(d);
      next.setDate(next.getDate() + 7);
      return next;
    });
  };

  const goToday = () => setViewDate(new Date());

  const navButtonClass = 'px-3 py-2 rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] text-[var(--text-primary)] text-sm font-semibold transition hover:bg-[var(--input-bg)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--link-primary)]';

  const viewLabel = viewMode === 'month'
    ? viewDate.toLocaleString(undefined, { month: 'long', year: 'numeric' })
    : `Week of ${startOfWeek(viewDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}`;

  return (
    <div className="space-y-4">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <button
            onClick={goPrev}
            className={navButtonClass}
            aria-label={viewMode === 'month' ? 'Previous month' : 'Previous week'}
          >
            ‹
          </button>
          <button
            onClick={goToday}
            className={navButtonClass}
            aria-label="Go to current date"
          >
            Today
          </button>
          <button
            onClick={goNext}
            className={navButtonClass}
            aria-label={viewMode === 'month' ? 'Next month' : 'Next week'}
          >
            ›
          </button>
        </div>

        <div className="flex flex-col items-center gap-2 sm:flex-row">
          <div className="text-lg font-semibold text-[var(--text-primary)]">{viewLabel}</div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setViewMode('month')}
              className={`px-3 py-2 rounded-lg border text-sm font-semibold transition ${viewMode === 'month'
                ? 'bg-[var(--link-primary)] text-white border-transparent'
                : 'bg-[var(--panel-bg)] text-[var(--text-primary)] border-[var(--panel-border)] hover:bg-[var(--input-bg)]'
                }`}
            >
              Month
            </button>
            <button
              type="button"
              onClick={() => setViewMode('week')}
              className={`px-3 py-2 rounded-lg border text-sm font-semibold transition ${viewMode === 'week'
                ? 'bg-[var(--link-primary)] text-white border-transparent'
                : 'bg-[var(--panel-bg)] text-[var(--text-primary)] border-[var(--panel-border)] hover:bg-[var(--input-bg)]'
                }`}
            >
              Week
            </button>
          </div>
        </div>
      </header>

      <div className="grid grid-cols-7 gap-1 text-xs text-[var(--text-subtle)] font-semibold">
        {WEEK_DAYS.map(day => (
          <div key={day} className="text-center py-2">
            {day}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1">
        {grid.map((week, weekIndex) => (
          <div key={weekIndex} className="contents">
            {week.map(day => {
              const dayKey = formatYMD(day);
              const isCurrentMonth = day.getMonth() === currentMonth && day.getFullYear() === currentYear;
              const isToday = dayKey === todayKey;
              const dayEvents = eventsByDate.get(dayKey) ?? [];

              return (
                <div
                  key={dayKey}
                  className={`min-h-[110px] rounded-xl border p-2 text-xs flex flex-col gap-1 transition-colors ${isCurrentMonth ? 'bg-[var(--panel-bg)]' : 'bg-[var(--input-bg)] text-[var(--text-subtle)]'
                    } ${isToday ? 'border-[var(--link-primary)] shadow-sm' : 'border-[var(--panel-border)]'} `}
                >
                  <div className="flex items-start justify-between">
                    <span className="font-semibold text-[var(--text-primary)]">{day.getDate()}</span>
                    {dayEvents.length > 0 && (
                      <span className="rounded-full bg-[var(--link-primary)] text-white px-2 py-0.5 text-[11px] font-semibold">
                        {dayEvents.length}
                      </span>
                    )}
                  </div>

                  <div className="flex-1 overflow-hidden">
                    {dayEvents.slice(0, 3).map(evt => (
                      <button
                        key={evt.id}
                        onClick={() => navigate(`/events/${evt.id}`)}
                        className="block text-left w-full rounded-lg px-2 py-1 text-[var(--text-body)] hover:bg-[var(--input-bg)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--link-primary)]"
                      >
                        <span className="block truncate font-medium">{evt.title}</span>
                        <span className="block text-[11px] text-[var(--text-subtle)]">
                          {formatEventStart(evt.startTime)}
                        </span>
                      </button>
                    ))}
                    {dayEvents.length > 3 && (
                      <div className="text-[11px] text-[var(--text-subtle)]">+ {dayEvents.length - 3} more</div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}
