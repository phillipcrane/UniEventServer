import { useNavigate } from 'react-router-dom';

export function DataDeletionPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[var(--dtu-primary-bg)] text-[var(--text-primary)]">
      <div className="sticky top-0 z-10 bg-[var(--dtu-secondary-bg)] border-b border-[var(--panel-border)] px-4 py-4 md:px-6">
        <button
          onClick={() => navigate('/')}
          className="text-[var(--link-primary)] hover:text-[var(--link-primary-hover)] font-semibold flex items-center gap-2"
        >
          ← Back to Events
        </button>
      </div>

      <div className="max-w-3xl mx-auto px-4 md:px-6 py-8 prose prose-invert">
        <h1 className="text-4xl font-bold mb-4">Data Deletion Instructions</h1>

        <p className="text-[var(--text-subtle)]">
          <strong>Last Updated:</strong> February 4, 2026
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">What DTUEvent stores</h2>
        <p>
          DTUEvent is a read-only event aggregator. We do <strong>not</strong> store personal user data for ordinary
          website visitors (no user accounts, no profiles).
        </p>
        <p>
          If you are a Facebook Page admin and you used “Link Facebook”, DTUEvent stores a <strong>Page access token</strong>
          (encrypted) and Page/event metadata needed to keep the Page’s public events synced.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">How to request deletion</h2>
        <p>
          If you previously linked a Facebook Page to DTUEvent, you can request deletion of the Page token and
          associated synced data in either of these ways:
        </p>

        <ol className="list-decimal pl-6 space-y-2">
          <li>
            <strong>Revoke access in Facebook</strong>
            <ul className="list-disc pl-6 mt-2">
              <li>Go to Facebook Settings → <strong>Business Integrations</strong> (or Apps and Websites)</li>
              <li>Find <strong>DTUEvent</strong></li>
              <li>Click <strong>Remove</strong> / <strong>Revoke</strong></li>
            </ul>
          </li>
          <li>
            <strong>Email us</strong>
            <ul className="list-disc pl-6 mt-2">
              <li>
                Email{' '}
                <a
                  href="mailto:philippzhuravlev@gmail.com"
                  className="text-[var(--link-primary)] hover:underline"
                >
                  philippzhuravlev@gmail.com
                </a>{' '}
                or{' '}
                <a
                  href="mailto:crillerhylle@gmail.com"
                  className="text-[var(--link-primary)] hover:underline"
                >
                  crillerhylle@gmail.com
                </a>
              </li>
              <li>Include the Facebook Page URL and/or Page ID you want removed</li>
            </ul>
          </li>
        </ol>

        <h2 className="text-2xl font-bold mt-8 mb-4">Deletion timeline</h2>
        <p>
          After revocation or a verified deletion request, we delete the Page access token (Google Cloud Secret
          Manager) and the associated Page/event data (Firestore) within <strong>24 hours</strong>.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">Related policies</h2>
        <p>
          See our full{' '}
          <a href="/privacy" className="text-[var(--link-primary)] hover:underline">
            Privacy Policy
          </a>{' '}
          for details on retention and third-party services.
        </p>
      </div>
    </div>
  );
}
