import { Link } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { ThemeToggle } from '../components/ThemeToggle';
import { Footer } from '../components/Footer';
import { Plus, UserPlus } from 'lucide-react';
import { useSignupPage } from '../hooks/useSignupPage';

export function SignupPage() {
    const navigate = useNavigate();
    const {
        username, setUsername,
        email, setEmail,
        password, setPassword,
        confirmPassword, setConfirmPassword,
        organizerPasswords,
        accountRole, setAccountRole,
        isRoleModalOpen, setIsRoleModalOpen,
        isLoading,
        errorMessage,
        updateOrganizerCode,
        addOrganizerCodeField,
        handleSubmit,
    } = useSignupPage();

    return (
        <div className="min-h-screen flex flex-col">
            <header className="page-header mx-6 md:mx-8 mt-4 md:mt-6 mb-8">
                <div className="header-content">
                    <HeaderLogoLink />
                    <div className="header-text">
                        <h1 className="header-title">Create Your Account</h1>
                        <p className="header-subtitle">Sign up to save your preferences and discover events faster</p>
                    </div>
                </div>

                <div className="header-toggle">
                    <ThemeToggle />
                </div>
            </header>

            <main className="flex-1 px-6 md:px-8 pb-8 max-w-6xl mx-auto w-full">
                <section className="relative flex min-h-[62vh] items-center justify-center">
                    <div className="auth-card relative w-full max-w-[680px] overflow-hidden rounded-[28px] border border-[var(--panel-border)]">
                        <div className="auth-card-glow pointer-events-none absolute inset-0" aria-hidden="true" />

                        <div className="relative z-[1] px-6 py-8 sm:px-9 sm:py-[2.3rem]">
                            <p className="m-0 text-xs font-bold tracking-[0.12em] text-[var(--text-subtle)]">NEW ACCOUNT</p>
                            <h2 className="mt-1.5 text-[clamp(1.55rem,2.7vw,2rem)] font-black leading-tight text-[var(--text-primary)]">Sign up to get started</h2>
                            <p className="mt-3 text-[0.95rem] text-[var(--text-subtle)]">Create your account with a username and password.</p>
                            <p className="mt-2 text-[0.9rem] font-semibold text-[var(--link-primary)]">Already have an account? Log in from the link below.</p>
                            <p className="mt-2 text-[0.85rem] font-bold text-[var(--text-subtle)]">
                                Account type: {accountRole === 'organizer' ? 'Organizer' : accountRole === 'user' ? 'User' : 'Not selected'}
                            </p>


                            <form className="mt-6 grid gap-3" onSubmit={handleSubmit} noValidate>
                                <label className="text-[0.85rem] font-bold uppercase tracking-[0.06em] text-[var(--text-primary)]" htmlFor="signup-username">Username</label>
                                <input
                                    id="signup-username"
                                    name="username"
                                    type="text"
                                    autoComplete="username"
                                    placeholder="Choose a username"
                                    className="auth-input"
                                    value={username}
                                    onChange={(e) => setUsername(e.target.value)}
                                />

                                <label className="text-[0.85rem] font-bold uppercase tracking-[0.06em] text-[var(--text-primary)]" htmlFor="signup-email">Email</label>
                                <input
                                    id="signup-email"
                                    name="email"
                                    type="email"
                                    autoComplete="email"
                                    placeholder="Enter your email"
                                    className="auth-input"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                />

                                <label className="text-[0.85rem] font-bold uppercase tracking-[0.06em] text-[var(--text-primary)]" htmlFor="signup-password">Password</label>
                                <input
                                    id="signup-password"
                                    name="password"
                                    type="password"
                                    autoComplete="new-password"
                                    placeholder="Create a password"
                                    className="auth-input"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                />

                                <label className="text-[0.85rem] font-bold uppercase tracking-[0.06em] text-[var(--text-primary)]" htmlFor="signup-confirm-password">Confirm Password</label>
                                <input
                                    id="signup-confirm-password"
                                    name="confirmPassword"
                                    type="password"
                                    autoComplete="new-password"
                                    placeholder="Type your password again"
                                    className="auth-input"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                />

                                {accountRole === 'organizer' && (
                                    <>
                                        <label className="text-[0.85rem] font-bold uppercase tracking-[0.06em] text-[var(--text-primary)]" htmlFor="signup-organizer-password-0">Organizer Access Password(s)</label>
                                        {organizerPasswords.map((code, index) => (
                                            <div key={index} className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2">
                                                <input
                                                    id={`signup-organizer-password-${index}`}
                                                    name={`organizerPassword-${index}`}
                                                    type="password"
                                                    autoComplete="off"
                                                    placeholder="Enter organizer access password"
                                                    className="auth-input"
                                                    value={code}
                                                    onChange={(e) => updateOrganizerCode(index, e.target.value)}
                                                />
                                                <button
                                                    type="button"
                                                    className="inline-flex h-[42px] w-[42px] items-center justify-center rounded-xl border border-[var(--input-border)] bg-[var(--input-bg)] text-[var(--text-primary)] transition hover:-translate-y-px hover:border-[var(--input-focus-border)] hover:bg-[var(--button-hover)]"
                                                    aria-label="Add organizer code field"
                                                    onClick={addOrganizerCodeField}
                                                >
                                                    <Plus size={16} />
                                                </button>
                                            </div>
                                        ))}

                                    </>
                                )}

                                {errorMessage && <p className="mt-0.5 text-[0.9rem] font-semibold text-[var(--dtu-accent)]">{errorMessage}</p>}

                                <div className="mt-3 grid gap-3">
                                    <button type="submit" className="auth-btn auth-btn-primary" disabled={isLoading}>
                                        <UserPlus size={18} />
                                        {isLoading ? 'Signing Up...' : accountRole ? `Sign Up as ${accountRole === 'organizer' ? 'Organizer' : 'User'}` : 'Sign Up'}
                                    </button>
                                </div>
                            </form>

                            <div className="mt-5 flex flex-wrap justify-center gap-4">
                                <Link to="/login" className="font-bold text-[var(--link-primary)] no-underline hover:text-[var(--link-primary-hover)] hover:underline">Already have an account? Log In</Link>
                                <Link to="/" className="font-bold text-[var(--link-primary)] no-underline hover:text-[var(--link-primary-hover)] hover:underline">← Back to Events</Link>
                            </div>
                        </div>
                    </div>
                </section>

                {isRoleModalOpen && (
                    <div
                        className="fixed inset-0 z-[70] flex items-center justify-center bg-[rgba(8,12,24,0.55)] p-4 backdrop-blur"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="signup-role-modal-title"
                        onClick={() => navigate('/login', { replace: true })}
                    >
                        <div className="w-full max-w-[460px] rounded-[20px] border border-[var(--panel-border)] bg-[var(--panel-bg)] p-5 shadow-[0_24px_46px_rgba(0,0,0,0.28)]" onClick={(e) => e.stopPropagation()}>
                            <h3 id="signup-role-modal-title" className="m-0 text-[1.2rem] font-extrabold text-[var(--text-primary)]">Choose account type</h3>
                            <p className="mt-2 text-[0.95rem] text-[var(--text-subtle)]">Do you want to sign up as User or Organizer?</p>

                            <div className="mt-4 grid grid-cols-2 gap-3">
                                <button
                                    type="button"
                                    className="auth-btn auth-btn-primary"
                                    onClick={() => {
                                        setAccountRole('user');
                                        setIsRoleModalOpen(false);
                                    }}
                                >
                                    User
                                </button>

                                <button
                                    type="button"
                                    className="auth-btn auth-btn-secondary"
                                    onClick={() => {
                                        setAccountRole('organizer');
                                        setIsRoleModalOpen(false);
                                    }}
                                >
                                    Organizer
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </main>

            <Footer />
        </div>
    );
}
