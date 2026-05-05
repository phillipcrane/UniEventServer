import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { ThemeToggle } from '../components/ThemeToggle';
import { Footer } from '../components/Footer';
import { ArrowLeft, CheckCircle, AlertCircle } from 'lucide-react';
import { useOrganizerSignupPage } from '../hooks/useOrganizerSignupPage';

export function OrganizerSignupPage() {
  const {
    currentUser,
    keyInput,
    setKeyInput,
    isVerifying,
    email,
    username,
    setUsername,
    password,
    setPassword,
    confirmPassword,
    setConfirmPassword,
    isRegistering,
    errorMessage,
    currentStep,
    setCurrentStep,
    showSuccessMessage,
    handleVerifyKey,
    handleUpgrade,
    handleRegister,
    navigate,
  } = useOrganizerSignupPage();

  return (
    <div className="min-h-screen flex flex-col">
      <header className="page-header mx-6 md:mx-8 mt-4 md:mt-6 mb-8">
        <div className="header-content">
          <HeaderLogoLink />
          <div className="header-text">
            <h1 className="header-title">Organizer Registration</h1>
            <p className="header-subtitle">Join as an event organizer with your invitation key</p>
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
              {currentStep === 1 ? (
                <>
                  <p className="signup-eyebrow">STEP 1 OF 2</p>
                  <h2 className="signup-title">Verify Your Invitation Key</h2>
                  <p className="signup-description">Enter your organizer invitation key to proceed.</p>

                  <form className="signup-form" onSubmit={handleVerifyKey} noValidate>
                    <label className="signup-label" htmlFor="organizer-key">Invitation Key</label>
                    <input
                      id="organizer-key"
                      name="organizer-key"
                      type="text"
                      autoComplete="off"
                      placeholder="Enter your invitation key"
                      className="signup-input"
                      value={keyInput}
                      onChange={(event) => setKeyInput(event.target.value)}
                    />

                    {errorMessage && (
                      <p className="signup-status signup-status-error">
                        <AlertCircle size={16} style={{ display: 'inline-block', marginRight: '8px' }} />
                        {errorMessage}
                      </p>
                    )}

                    <div className="signup-actions">
                      <button type="submit" className="signup-btn signup-btn-primary" disabled={isVerifying}>
                        {isVerifying ? 'Verifying...' : 'Verify Key'}
                      </button>

                      <button
                        type="button"
                        className="signup-btn signup-btn-secondary"
                        onClick={() => navigate('/signup-organizer-landing')}
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                </>
              ) : currentUser ? (
                <>
                  <p className="signup-eyebrow">STEP 2 OF 2</p>
                  <h2 className="signup-title">Upgrade Your Account</h2>
                  <p className="signup-description">
                    You're signed in as <strong>{currentUser.email}</strong>. Click below to upgrade your account to organizer.
                  </p>

                  <form className="signup-form" onSubmit={handleUpgrade} noValidate>
                    {errorMessage && (
                      <p className="signup-status signup-status-error">
                        <AlertCircle size={16} style={{ display: 'inline-block', marginRight: '8px' }} />
                        {errorMessage}
                      </p>
                    )}

                    {showSuccessMessage && (
                      <p className="signup-status signup-status-success">
                        <CheckCircle size={16} style={{ display: 'inline-block', marginRight: '8px' }} />
                        Account upgraded! Redirecting to your profile...
                      </p>
                    )}

                    <div className="signup-actions">
                      <button type="submit" className="signup-btn signup-btn-primary" disabled={isRegistering || showSuccessMessage}>
                        {isRegistering ? 'Upgrading...' : 'Upgrade to Organizer'}
                      </button>

                      <button
                        type="button"
                        className="signup-btn signup-btn-secondary"
                        onClick={() => setCurrentStep(1)}
                        disabled={isRegistering}
                      >
                        <ArrowLeft size={16} />
                        Go Back
                      </button>
                    </div>
                  </form>
                </>
              ) : (
                <>
                  <p className="signup-eyebrow">STEP 2 OF 2</p>
                  <h2 className="signup-title">Complete Your Registration</h2>
                  <p className="signup-description">You're almost there. Fill in your account details.</p>

                  <form className="signup-form" onSubmit={handleRegister} noValidate>
                    <label className="signup-label" htmlFor="organizer-email">Email</label>
                    <input
                      id="organizer-email"
                      name="email"
                      type="email"
                      className="signup-input"
                      value={email}
                      disabled
                      style={{ opacity: 0.6, cursor: 'not-allowed' }}
                    />

                    <label className="signup-label" htmlFor="organizer-username">Username</label>
                    <input
                      id="organizer-username"
                      name="username"
                      type="text"
                      autoComplete="username"
                      placeholder="Choose a username"
                      className="signup-input"
                      value={username}
                      onChange={(event) => setUsername(event.target.value)}
                    />

                    <label className="signup-label" htmlFor="organizer-password">Password</label>
                    <input
                      id="organizer-password"
                      name="password"
                      type="password"
                      autoComplete="new-password"
                      placeholder="Create a password (at least 12 characters)"
                      className="signup-input"
                      value={password}
                      onChange={(event) => setPassword(event.target.value)}
                    />

                    <label className="signup-label" htmlFor="organizer-confirm-password">Confirm Password</label>
                    <input
                      id="organizer-confirm-password"
                      name="confirmPassword"
                      type="password"
                      autoComplete="new-password"
                      placeholder="Type your password again"
                      className="signup-input"
                      value={confirmPassword}
                      onChange={(event) => setConfirmPassword(event.target.value)}
                    />

                    {errorMessage && (
                      <p className="signup-status signup-status-error">
                        <AlertCircle size={16} style={{ display: 'inline-block', marginRight: '8px' }} />
                        {errorMessage}
                      </p>
                    )}

                    {showSuccessMessage && (
                      <p className="signup-status signup-status-success">
                        <CheckCircle size={16} style={{ display: 'inline-block', marginRight: '8px' }} />
                        Account created. Redirecting to login...
                      </p>
                    )}

                    <div className="signup-actions">
                      <button type="submit" className="signup-btn signup-btn-primary" disabled={isRegistering || showSuccessMessage}>
                        {isRegistering ? 'Creating Account...' : 'Complete Registration'}
                      </button>

                      <button
                        type="button"
                        className="signup-btn signup-btn-secondary"
                        onClick={() => setCurrentStep(1)}
                        disabled={isRegistering}
                      >
                        <ArrowLeft size={16} />
                        Go Back
                      </button>
                    </div>
                  </form>
                </>
              )}
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
