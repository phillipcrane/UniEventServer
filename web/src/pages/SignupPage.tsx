import { Link } from 'react-router-dom';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { ThemeToggle } from '../components/ThemeToggle';
import { Footer } from '../components/Footer';
import { UserPlus, UsersRound } from 'lucide-react';
import { useSignupPage } from '../hooks/useSignupPage';
import { useNavigate } from 'react-router-dom';

export function SignupPage() {
  const {
    username,
    setUsername,
    email,
    setEmail,
    password,
    setPassword,
    confirmPassword,
    setConfirmPassword,
    organizerPasswords,
    accountRole,
    setAccountRole,
    isRoleModalOpen,
    setIsRoleModalOpen,
    isLoading,
    errorMessage,
    updateOrganizerCode,
    addOrganizerCodeField,
    handleSubmit,
  } = useSignupPage();
  const navigate = useNavigate();

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
        <section className="signup-shell">
          <div className="signup-card">
            <div className="signup-card-glow" aria-hidden="true" />

            <div className="signup-card-content">
              {isRoleModalOpen && (
                <section role="dialog" aria-modal="true" onClick={() => navigate('/login', { replace: true })} className="mb-6 rounded-2xl border border-[var(--panel-border)] bg-[var(--input-bg)]/75 p-4 shadow-sm">
                  <div onClick={(e) => e.stopPropagation()}>
                    <p className="signup-eyebrow">ACCOUNT TYPE</p>
                    <h2 className="signup-title mt-2">Do you want to sign up as User or Organizer?</h2>
                    <p className="signup-description mt-2">
                      Regular users can sign up immediately. Organizers can use the invitation-key flow if they have one.
                    </p>

                    <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    <button
                      type="button"
                      onClick={() => {
                        setAccountRole('user');
                        setIsRoleModalOpen(false);
                      }}
                      className="rounded-xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-4 text-left transition-colors duration-200 hover:bg-[var(--button-hover)]"
                    >
                      <p className="text-sm font-semibold text-[var(--text-subtle)]">User</p>
                      <h3 className="mt-1 text-lg font-bold text-[var(--text-primary)]">Sign up as a user</h3>
                      <p className="mt-2 text-sm text-[var(--text-subtle)]">Get a normal account for saving events and using the site.</p>
                    </button>

                    <button
                      type="button"
                      onClick={() => {
                        setAccountRole('organizer');
                        setIsRoleModalOpen(false);
                      }}
                      className="rounded-xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-4 text-left transition-colors duration-200 hover:bg-[var(--button-hover)]"
                    >
                      <p className="text-sm font-semibold text-[var(--text-subtle)]">Organizer</p>
                      <h3 className="mt-1 text-lg font-bold text-[var(--text-primary)]">Use an organizer key</h3>
                      <p className="mt-2 text-sm text-[var(--text-subtle)]">If you already have a key, you can finish the organizer signup flow here.</p>
                    </button>
                  </div>
                  </div>

                  <div className="mt-4 flex flex-wrap items-center gap-2">
                    <Link to="/signup-organizer-landing" className="signup-link">Need an organizer key?</Link>
                    <button type="button" className="signup-link" onClick={() => setIsRoleModalOpen(false)}>
                      Continue without choosing now
                    </button>
                  </div>
                </section>
              )}

              <p className="signup-eyebrow">NEW ACCOUNT</p>
              <h2 className="signup-title">Sign up to get started</h2>
              <p className="signup-description">Create your account with a username and password.</p>
              <p className="signup-helper">
                Already have an account? Log in from the link below.
                {accountRole && (
                  <span className="block mt-1 text-[var(--text-subtle)]">
                    Signing up as {accountRole === 'organizer' ? 'an organizer' : 'a user'}.
                  </span>
                )}
              </p>

              <form className="signup-form" onSubmit={handleSubmit} noValidate>
                <label className="signup-label" htmlFor="signup-username">Username</label>
                <input
                  id="signup-username"
                  name="username"
                  type="text"
                  autoComplete="username"
                  placeholder="Choose a username"
                  className="signup-input"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                />

                <label className="signup-label" htmlFor="signup-email">Email</label>
                <input
                  id="signup-email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  placeholder="Enter your email"
                  className="signup-input"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />

                <label className="signup-label" htmlFor="signup-password">Password</label>
                <input
                  id="signup-password"
                  name="password"
                  type="password"
                  autoComplete="new-password"
                  placeholder="Create a password"
                  className="signup-input"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />

                <label className="signup-label" htmlFor="signup-confirm-password">Confirm Password</label>
                <input
                  id="signup-confirm-password"
                  name="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  placeholder="Type your password again"
                  className="signup-input"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                />

                {accountRole === 'organizer' && (
                  <section className="mt-3 rounded-xl border border-[var(--panel-border)] bg-[var(--input-bg)]/70 p-4">
                    <div className="flex items-center gap-2">
                      <UsersRound size={16} />
                      <p className="text-sm font-semibold text-[var(--text-primary)]">Organizer invitation keys</p>
                    </div>
                    <p className="mt-1 text-xs text-[var(--text-subtle)]">
                      Enter one or more key codes if your invitation requires them.
                    </p>

                    <label className="signup-label mt-3" htmlFor="organizer-access-0">Organizer Access Password(s)</label>
                    <div className="mt-2 space-y-3">
                      {organizerPasswords.map((code, index) => (
                        <input
                          key={index}
                          id={`organizer-access-${index}`}
                          aria-label={index === 0 ? 'Organizer Access Password(s)' : undefined}
                          type="text"
                          className="signup-input"
                          placeholder="Enter organizer access key"
                          value={code}
                          onChange={(event) => updateOrganizerCode(index, event.target.value)}
                        />
                      ))}
                    </div>

                    <button
                      type="button"
                      onClick={addOrganizerCodeField}
                      className="mt-3 signup-link"
                    >
                      Add another key field
                    </button>
                  </section>
                )}

                {errorMessage && <p className="signup-status signup-status-error">{errorMessage}</p>}

                <div className="signup-actions">
                  <button type="submit" className="signup-btn signup-btn-primary" disabled={isLoading}>
                    <UserPlus size={18} />
                    {isLoading ? 'Signing Up...' : (accountRole === 'organizer' ? 'Sign Up as Organizer' : (accountRole === 'user' ? 'Sign Up as User' : 'Sign Up'))}
                  </button>
                </div>
              </form>

              <div className="signup-links-row">
                <Link to="/login" className="signup-link">Already have an account? Log In</Link>
                <Link to="/signup-organizer-landing" className="signup-link">Want to become an organizer?</Link>
                <Link to="/" className="signup-link">← Back to Events</Link>
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
