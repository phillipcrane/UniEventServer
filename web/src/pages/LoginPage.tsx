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
        <section className="login-shell">
          <div className="login-card">
            <div className="login-card-glow" aria-hidden="true" />

            <div className="login-card-content">
              <p className="login-eyebrow">AUTHENTICATION</p>
              <h2 className="login-title">Sign in to your account</h2>
              <p className="login-description">Enter your email and password to continue.</p>
              <p className="login-helper">No account yet? Use the Sign Up link below.</p>

              <form className="login-form" onSubmit={handleSubmit} noValidate>
                <label className="login-label" htmlFor="email">Email</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  placeholder="Enter your email"
                  className="login-input"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />

                <label className="login-label" htmlFor="password">Password</label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  className="login-input"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />

                {errorMessage && <p className="login-status login-status-error">{errorMessage}</p>}

                <div className="login-actions">
                  <button type="submit" className="login-btn login-btn-primary" disabled={isLoading}>
                    <LogIn size={18} />
                    {isLoading ? 'Signing In...' : 'Sign In'}
                  </button>

                  <Link to="/signup" className="login-btn login-btn-secondary">
                    <UserPlus size={18} />
                    Sign Up
                  </Link>
                </div>
              </form>

              <div className="login-back-row">
                <Link to="/" className="login-back-link">← Back to Events</Link>
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
