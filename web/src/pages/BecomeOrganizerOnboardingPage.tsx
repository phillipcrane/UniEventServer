import { Link } from 'react-router-dom';
import { BadgeCheck, CalendarCheck2, FileText, KeyRound, ShieldCheck, Sparkles } from 'lucide-react';
import { Footer } from '../components/Footer';
import { HeaderLogoLink } from '../components/HeaderLogoLink';
import { ThemeToggle } from '../components/ThemeToggle';

export function BecomeOrganizerOnboardingPage() {
  return (
    <div className="organizer-onboarding-page min-h-screen flex flex-col">
      <header className="page-header mx-6 md:mx-8 mt-4 md:mt-6 mb-8">
        <div className="header-content">
          <HeaderLogoLink />
          <div className="header-text">
            <h1 className="header-title">Become an Organizer</h1>
            <p className="header-subtitle">Onboarding hub for creating and managing university events</p>
          </div>
        </div>

        <div className="header-toggle">
          <ThemeToggle />
        </div>
      </header>

      <main className="flex-1 px-6 md:px-8 pb-10 max-w-6xl mx-auto w-full">
        <section className="organizer-onboarding-shell" aria-label="Organizer onboarding content">
          <div className="organizer-onboarding-card">
            <div className="organizer-onboarding-glow" aria-hidden="true" />

            <div className="organizer-onboarding-content">
              <div className="organizer-onboarding-intro">
                <p className="organizer-onboarding-eyebrow">ORGANIZER PROGRAM</p>
                <h1 className="organizer-onboarding-title">Launch Your Organizer Profile</h1>
                <p className="organizer-onboarding-description">
                  Becoming an organizer is simple: request a key through our Google Form, wait for review, and complete your registration. If you are eligible, you will receive your key by email within 24 hours.
                </p>
              </div>

              <div className="organizer-onboarding-highlight" role="status" aria-live="polite">
                <Sparkles size={16} />
                <span>Fast overview: request by Google Form first, then register with the key code.</span>
              </div>

              <div className="organizer-onboarding-grid" role="list" aria-label="Onboarding steps">
                <article className="organizer-step" role="listitem">
                  <div className="organizer-step-icon" aria-hidden="true">
                    <KeyRound size={18} />
                  </div>
                  <h3>Step 1: Request a Key</h3>
                  <p>Open the Google Form and fill in your organization details, Facebook page links, and proof of eligibility.</p>
                </article>

                <article className="organizer-step" role="listitem">
                  <div className="organizer-step-icon" aria-hidden="true">
                    <BadgeCheck size={18} />
                  </div>
                  <h3>Step 2: We Review in 24 Hours</h3>
                  <p>We verify that you are eligible, including your relation to the Facebook page you manage.</p>
                </article>

                <article className="organizer-step" role="listitem">
                  <div className="organizer-step-icon" aria-hidden="true">
                    <ShieldCheck size={18} />
                  </div>
                  <h3>Step 3: Receive Key by Email</h3>
                  <p>If approved, you get a 32-character key code. Keep it secure and use it to continue registration.</p>
                </article>

                <article className="organizer-step" role="listitem">
                  <div className="organizer-step-icon" aria-hidden="true">
                    <CalendarCheck2 size={18} />
                  </div>
                  <h3>Step 4: Create Account and Publish</h3>
                  <p>Use your key code to create your organizer account, then start creating and managing events.</p>
                </article>
              </div>

              <section className="organizer-onboarding-checklist" aria-label="What you need">
                <h3>
                  <FileText size={16} />
                  What to Prepare Before Requesting
                </h3>
                <ul>
                  <li>Your full name, email, and phone number</li>
                  <li>Organization name and role</li>
                  <li>Organization Facebook page and your personal Facebook profile</li>
                  <li>Short proof of eligibility and an optional comment</li>
                </ul>
              </section>

              <section className="organizer-onboarding-checklist" aria-label="Already have a key">
                <h3>
                  <ShieldCheck size={16} />
                  Already Have a Key Code?
                </h3>
                <ul>
                  <li>If you already received your key code, skip the request form.</li>
                  <li>Go directly to the key verification page and complete registration.</li>
                  <li>A valid email address</li>
                  <li>A secure password (minimum 12 characters)</li>
                </ul>
              </section>

              <div className="organizer-onboarding-actions">
                <Link to="/" className="organizer-btn organizer-btn-ghost">
                  Back to Home
                </Link>
                <Link to="/signup-organizer-landing" className="organizer-btn organizer-btn-primary">
                  Continue to Organizer Signup
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
