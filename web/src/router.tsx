import { lazy, Suspense } from 'react';
import { createBrowserRouter } from 'react-router-dom';
import { MainPage } from './pages/MainPage.tsx';
import { EventPage } from './pages/EventPage';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignupPage';
import { ProfilePage } from './pages/ProfilePage.tsx';

// Legal and organizer pages are rarely visited - lazy-load them so they
// don't bloat the initial bundle that every visitor pays to parse.
const TermsAndConditionsPage = lazy(() =>
  import('./pages/TermsAndConditionsPage').then(m => ({ default: m.TermsAndConditionsPage }))
);
const PrivacyPolicyPage = lazy(() =>
  import('./pages/PrivacyPolicyPage').then(m => ({ default: m.PrivacyPolicyPage }))
);
const DataDeletionPage = lazy(() =>
  import('./pages/DataDeletionPage').then(m => ({ default: m.DataDeletionPage }))
);
const ManualEventPage = lazy(() =>
  import('./pages/ManualEventPage.tsx').then(m => ({ default: m.ManualEventPage }))
);
const OrganizerSignupLandingPage = lazy(() =>
  import('./pages/OrganizerSignupLandingPage.tsx').then(m => ({ default: m.OrganizerSignupLandingPage }))
);
const OrganizerSignupPage = lazy(() =>
  import('./pages/OrganizerSignupPage.tsx').then(m => ({ default: m.OrganizerSignupPage }))
);
const BecomeOrganizerOnboardingPage = lazy(() =>
  import('./pages/BecomeOrganizerOnboardingPage.tsx').then(m => ({ default: m.BecomeOrganizerOnboardingPage }))
);
const GenerateOrganizerKeyPage = lazy(() =>
  import('./pages/admin/GenerateOrganizerKeyPage.tsx').then(m => ({ default: m.GenerateOrganizerKeyPage }))
);

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainPage />,
  },
  {
    path: '/events/:id',
    element: <EventPage />,
  },
  {
    path: '/terms',
    element: <Suspense fallback={null}><TermsAndConditionsPage /></Suspense>,
  },
  {
    path: '/privacy',
    element: <Suspense fallback={null}><PrivacyPolicyPage /></Suspense>,
  },
  {
    path: '/data-deletion',
    element: <Suspense fallback={null}><DataDeletionPage /></Suspense>,
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/signup',
    element: <SignupPage />,
  },
  {
    path: '/signup-organizer-landing',
    element: <Suspense fallback={null}><OrganizerSignupLandingPage /></Suspense>,
  },
  {
    path: '/signup-organizer',
    element: <Suspense fallback={null}><OrganizerSignupPage /></Suspense>,
  },
  {
    path: '/organizer/onboarding',
    element: <Suspense fallback={null}><BecomeOrganizerOnboardingPage /></Suspense>,
  },
  {
    path: '/admin/generate-organizer-key',
    element: <GenerateOrganizerKeyPage />,
  },
  {
    path: '/profile',
    element: <ProfilePage />,
  },
  {
    path: '/organizer/events/new',
    element: <Suspense fallback={null}><ManualEventPage /></Suspense>,
  },
]);
