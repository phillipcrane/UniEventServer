import { Link } from 'react-router-dom';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { ThemeToggle } from '../components/ThemeToggle';
import { Footer } from '../components/Footer';
import { LogIn, UserPlus } from 'lucide-react';
import { useLoginPage } from '../hooks/useLoginPage';

export function LoginPage() {
  const { email, setEmail, password, setPassword, isLoading, errorMessage, handleSubmit } = useLoginPage();

  return (
    <div className="min-h-screen flex flex-col">
      <header className="page-header mx-6 md:mx-8 mt-4 md:mt-6 mb-8">
        <div className="header-content">
          <HeaderLogoLink />
          <div className="header-text">
            <h1 className="header-title">Welcome Back</h1>
            <p className="header-subtitle">Log in to continue your UniEvent experience</p>
          </div>
        </div>

        <div className="header-toggle">
          <ThemeToggle />
        </div>
      </header>

      <main className="flex-1 px-6 md:px-8 pb-8 max-w-6xl mx-auto w-full">
        <section className="flex justify-center py-8">
          <div className="auth-card w-full max-w-[480px] relative overflow-hidden rounded-3xl p-10">
            <div className="auth-card-glow absolute inset-0 pointer-events-none" aria-hidden="true" />

            <div className="relative z-10">
              <p className="text-[0.7rem] font-bold tracking-[0.14em] uppercase text-[var(--text-subtle)] mb-2">AUTHENTICATION</p>
              <h2 className="text-[1.6rem] font-extrabold tracking-tight text-[var(--text-primary)] mb-1">Sign in to your account</h2>
              <p className="text-sm text-[var(--text-subtle)] mb-1">Enter your email and password to continue.</p>
              <p className="text-xs text-[var(--text-subtle)] mb-7">No account yet? Use the Sign Up link below.</p>

              <form className="flex flex-col gap-[0.85rem]" onSubmit={handleSubmit} noValidate>
                <label className="text-sm font-semibold text-[var(--text-primary)]" htmlFor="email">Email</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  placeholder="Enter your email"
                  className="auth-input"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />

                <label className="text-sm font-semibold text-[var(--text-primary)]" htmlFor="password">Password</label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  className="auth-input"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />

                {errorMessage && (
                  <p className="text-sm font-semibold text-[var(--dtu-accent)]">{errorMessage}</p>
                )}

                <div className="flex flex-col gap-3 mt-2">
                  <button type="submit" className="auth-btn auth-btn-primary w-full" disabled={isLoading}>
                    <LogIn size={18} />
                    {isLoading ? 'Signing In...' : 'Sign In'}
                  </button>

                  <Link to="/signup" className="auth-btn auth-btn-secondary w-full">
                    <UserPlus size={18} />
                    Sign Up
                  </Link>
                </div>
              </form>

              <div className="mt-6 text-center">
                <Link
                  to="/"
                  className="text-sm text-[var(--link-primary)] transition-colors hover:text-[var(--link-primary-hover)] hover:underline"
                >
                  ← Back to Events
                </Link>
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
