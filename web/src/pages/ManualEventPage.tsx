import { Link } from 'react-router-dom';
import { CalendarDays, FilePlus2, Image as ImageIcon, Link as LinkIcon, MapPin, Save, Tags, Ticket } from 'lucide-react';
import { Footer } from '../components/Footer';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { ThemeToggle } from '../components/ThemeToggle';

export function ManualEventPage() {
    return (
        <div className="min-h-screen flex flex-col">
            <header className="page-header mx-6 md:mx-8 mt-4 md:mt-6 mb-8">
                <div className="header-content">
                    <HeaderLogoLink />
                    <div className="header-text">
                        <h1 className="header-title">Create Manual Event</h1>
                        <p className="header-subtitle">Organizer draft form for adding an event manually</p>
                    </div>
                </div>

                <div className="header-toggle">
                    <ThemeToggle />
                </div>
            </header>

            <main className="flex-1 px-6 md:px-8 pb-10 max-w-6xl mx-auto w-full">
                <section className="relative flex min-h-[64vh] items-start justify-center">
                    <div className="auth-card relative w-full max-w-[980px] overflow-hidden rounded-[30px] border border-[var(--panel-border)]">
                        <div className="auth-card-glow pointer-events-none absolute inset-0" aria-hidden="true" />

                        <div className="relative z-[1] px-4 py-7 sm:px-7 sm:py-8 lg:px-8 lg:py-8">
                            <div className="flex flex-wrap items-start justify-between gap-4">
                                <div>
                                    <p className="m-0 text-[0.74rem] font-extrabold tracking-[0.14em] text-[var(--text-subtle)]">ORGANIZER TOOLS</p>
                                    <h2 className="mt-1.5 text-[clamp(1.45rem,2.35vw,2rem)] font-black leading-tight text-[var(--text-primary)]">Manual Event Builder</h2>
                                    <p className="mt-3 max-w-[66ch] text-[0.95rem] text-[var(--text-subtle)]">
                                        This is UI-only for now. Save and publish actions are intentionally disabled until backend logic is wired.
                                    </p>
                                </div>
                                <div className="mt-1 inline-flex items-center gap-1.5 rounded-full border border-[var(--panel-border)] bg-[rgba(232,93,59,0.13)] px-3.5 py-2 text-[0.78rem] font-bold text-[var(--text-primary)]" aria-label="UI only mode">
                                    <FilePlus2 size={16} />
                                    Draft UI
                                </div>
                            </div>

                            <form className="mt-5 grid gap-4 lg:gap-[1.05rem]" onSubmit={(event) => event.preventDefault()}>
                                <section className="rounded-[18px] border border-[var(--panel-border)] bg-[color-mix(in_srgb,var(--panel-bg)_74%,var(--input-bg)_26%)] p-4" aria-label="Basic details">
                                    <h3 className="m-0 inline-flex items-center gap-2 text-[0.95rem] font-extrabold uppercase tracking-[0.04em] text-[var(--text-primary)]">Basic Details</h3>
                                    <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
                                        <label className="grid gap-1.5">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Event title</span>
                                            <input type="text" placeholder="DTU Robotics Night 2026" className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Organizer display name</span>
                                            <input type="text" placeholder="DTU Robotics Society" className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Category</span>
                                            <select className="auth-input">
                                                <option>Workshop</option>
                                                <option>Conference</option>
                                                <option>Hackathon</option>
                                                <option>Career</option>
                                                <option>Social</option>
                                            </select>
                                        </label>

                                        <label className="grid gap-1.5">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Audience</span>
                                            <select className="auth-input">
                                                <option>Open to everyone</option>
                                                <option>Students only</option>
                                                <option>Staff only</option>
                                                <option>Invite only</option>
                                            </select>
                                        </label>
                                    </div>
                                </section>

                                <section className="rounded-[18px] border border-[var(--panel-border)] bg-[color-mix(in_srgb,var(--panel-bg)_74%,var(--input-bg)_26%)] p-4" aria-label="Date and location">
                                    <h3 className="m-0 inline-flex items-center gap-2 text-[0.95rem] font-extrabold uppercase tracking-[0.04em] text-[var(--text-primary)]">
                                        <CalendarDays size={16} />
                                        Date and Location
                                    </h3>
                                    <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
                                        <label className="grid gap-1.5">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Start date</span>
                                            <input type="date" className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Start time</span>
                                            <input type="time" className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">End date</span>
                                            <input type="date" className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">End time</span>
                                            <input type="time" className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5 sm:col-span-2">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">
                                                <MapPin size={14} />
                                                Venue name
                                            </span>
                                            <input type="text" placeholder="Oticon Hall, Building 302" className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5 sm:col-span-2">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Address</span>
                                            <input type="text" placeholder="Anker Engelunds Vej 1, 2800 Kongens Lyngby" className="auth-input" />
                                        </label>
                                    </div>
                                </section>

                                <section className="rounded-[18px] border border-[var(--panel-border)] bg-[color-mix(in_srgb,var(--panel-bg)_74%,var(--input-bg)_26%)] p-4" aria-label="Media and registration">
                                    <h3 className="m-0 inline-flex items-center gap-2 text-[0.95rem] font-extrabold uppercase tracking-[0.04em] text-[var(--text-primary)]">
                                        <ImageIcon size={16} />
                                        Media and Registration
                                    </h3>
                                    <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
                                        <label className="grid gap-1.5 sm:col-span-2">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Cover image URL</span>
                                            <input type="url" placeholder="https://..." className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5 sm:col-span-2">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">
                                                <LinkIcon size={14} />
                                                External event link
                                            </span>
                                            <input type="url" placeholder="https://facebook.com/events/..." className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">
                                                <Ticket size={14} />
                                                Ticket type
                                            </span>
                                            <select className="auth-input">
                                                <option>Free</option>
                                                <option>Paid</option>
                                                <option>RSVP only</option>
                                            </select>
                                        </label>

                                        <label className="grid gap-1.5">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Capacity</span>
                                            <input type="number" placeholder="120" className="auth-input" min={0} />
                                        </label>
                                    </div>
                                </section>

                                <section className="rounded-[18px] border border-[var(--panel-border)] bg-[color-mix(in_srgb,var(--panel-bg)_74%,var(--input-bg)_26%)] p-4" aria-label="Description and tags">
                                    <h3 className="m-0 inline-flex items-center gap-2 text-[0.95rem] font-extrabold uppercase tracking-[0.04em] text-[var(--text-primary)]">
                                        <Tags size={16} />
                                        Content
                                    </h3>
                                    <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
                                        <label className="grid gap-1.5 sm:col-span-2">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Short summary</span>
                                            <input type="text" placeholder="A fast introduction to autonomous drone systems." className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5 sm:col-span-2">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Tags</span>
                                            <input type="text" placeholder="robotics, ai, drones, engineering" className="auth-input" />
                                        </label>

                                        <label className="grid gap-1.5 sm:col-span-2">
                                            <span className="inline-flex items-center gap-1 text-[0.8rem] font-bold uppercase tracking-[0.04em] text-[var(--text-primary)]">Full description</span>
                                            <textarea
                                                className="auth-input min-h-[140px] resize-y"
                                                rows={7}
                                                placeholder="Describe agenda, speakers, expectations, and practical details..."
                                            />
                                        </label>
                                    </div>
                                </section>

                                <div className="mt-1 grid grid-cols-1 gap-3 sm:grid-cols-3">
                                    <Link to="/profile" className="auth-btn border-[var(--panel-border)] bg-[var(--panel-bg)] text-[var(--text-primary)] no-underline">
                                        Back to Profile
                                    </Link>

                                    <button type="button" className="auth-btn auth-btn-secondary" disabled>
                                        <Save size={16} />
                                        Save Draft (Soon)
                                    </button>

                                    <button type="submit" className="auth-btn auth-btn-primary" disabled>
                                        Publish Event (Soon)
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </section>
            </main>

            <Footer />
        </div>
    );
}
