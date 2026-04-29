import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { ThemeToggle } from '../components/ThemeToggle';
import { Footer } from '../components/Footer';
import { LogIn, UserPlus } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { isValidEmail } from '../utils/validationUtils';

export function LoginPage() {
    const navigate = useNavigate();
    const { login, isLoading, error, clearError } = useAuth();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [validationError, setValidationError] = useState('');

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setValidationError('');
        clearError();

        const trimmedEmail = email.trim();
        if (!trimmedEmail || !password) {
            setValidationError('Please provide both email and password.');
            return;
        }

        if (!isValidEmail(trimmedEmail)) {
            setValidationError('Please provide a valid email address.');
            return;
        }

        try {
            await login(trimmedEmail, password);
            navigate('/', { replace: true });
        } catch (error) {
            void error;
        } finally {
            // loading state handled by context
        }
    }

    return (
        <div className="min-h-screen flex flex-col">
            <header className="page-header mx-6 md:mx-8 mt-4 md:mt-6 mb-8">
                <div className="header-content">
                    <HeaderLogoLink />
                    <div className="header-text">
                        <h1 className="header-title">Welcome Back</h1>
                        <p className="header-subtitle">Log in to continue your DTU Events experience</p>
                    </div>
                </div>

                <div className="header-toggle">
                    <ThemeToggle />
                </div>
            </header>

            <main className="flex-1 px-6 md:px-8 pb-8 max-w-6xl mx-auto w-full">
                <section className="relative flex min-h-[62vh] items-center justify-center">
                    <div className="auth-card relative w-full max-w-[640px] overflow-hidden rounded-[28px] border border-[var(--panel-border)]">
                        <div className="auth-card-glow pointer-events-none absolute inset-0" aria-hidden="true" />

                        <div className="relative z-[1] px-6 py-8 sm:px-9 sm:py-[2.3rem]">
                            <p className="m-0 text-xs font-bold tracking-[0.12em] text-[var(--text-subtle)]">AUTHENTICATION</p>
                            <h2 className="mt-1.5 text-[clamp(1.55rem,2.7vw,2rem)] font-black leading-tight text-[var(--text-primary)]">Sign in to your account</h2>
                            <p className="mt-3 text-[0.95rem] text-[var(--text-subtle)]">Enter your email and password to continue.</p>
                            <p className="mt-2 text-[0.9rem] font-semibold text-[var(--link-primary)]">No account yet? Use the Sign Up link below.</p>

                            <form className="mt-6 grid gap-3" onSubmit={handleSubmit} noValidate>
                                <label className="text-[0.85rem] font-bold uppercase tracking-[0.06em] text-[var(--text-primary)]" htmlFor="email">Email</label>
                                <input
                                    id="email"
                                    name="email"
                                    type="email"
                                    autoComplete="email"
                                    placeholder="Enter your email"
                                    className="auth-input"
                                    value={email}
                                    onChange={(event) => {
                                        setEmail(event.target.value);
                                        if (validationError) setValidationError('');
                                        clearError();
                                    }}
                                />

                                <label className="text-[0.85rem] font-bold uppercase tracking-[0.06em] text-[var(--text-primary)]" htmlFor="password">Password</label>
                                <input
                                    id="password"
                                    name="password"
                                    type="password"
                                    autoComplete="current-password"
                                    placeholder="Enter your password"
                                    className="auth-input"
                                    value={password}
                                    onChange={(event) => {
                                        setPassword(event.target.value);
                                        if (validationError) setValidationError('');
                                        clearError();
                                    }}
                                />

                                {(validationError || error) && (
                                    <p className="mt-0.5 text-[0.9rem] font-semibold text-[var(--dtu-accent)]">
                                        {validationError || error}
                                    </p>
                                )}

                                <div className="mt-2 grid gap-3 sm:grid-cols-2">
                                    <button type="submit" className="auth-btn auth-btn-primary" disabled={isLoading}>
                                        <LogIn size={18} />
                                        {isLoading ? 'Signing In...' : 'Sign In'}
                                    </button>

                                    <Link to="/signup" className="auth-btn auth-btn-secondary">
                                        <UserPlus size={18} />
                                        Sign Up
                                    </Link>
                                </div>
                            </form>

                            <div className="mt-5 flex justify-center">
                                <Link to="/" className="font-bold text-[var(--link-primary)] no-underline hover:text-[var(--link-primary-hover)] hover:underline">← Back to Events</Link>
                            </div>
                        </div>
                    </div>
                </section>
            </main>

            <Footer />
        </div>
    );
}
