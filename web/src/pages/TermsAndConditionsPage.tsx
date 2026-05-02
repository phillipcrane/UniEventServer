import { useNavigate } from 'react-router-dom';

export function TermsAndConditionsPage() {
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
        <h1 className="text-4xl font-bold mb-4">Terms and Conditions</h1>

        <p className="text-[var(--text-subtle)]">
          <strong>Last Updated:</strong> February 12, 2026<br />
          <strong>Effective Date:</strong> February 12, 2026
        </p>

        <p>
          These Terms and Conditions (“Terms”) govern your access to and use of the UniEvent service (“Service”).
          By accessing or using the Service, you agree to be bound by these Terms. If you do not agree to any part of
          these Terms, you may not use the Service.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">1. Definition of the Service</h2>
        <p>
          UniEvent is a read-only, public event aggregation platform that displays public event information sourced
          from external platforms such as Facebook. The Service does not provide user accounts, social interactions,
          or advertising features.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">2. Acceptable Use</h2>
        <p>
          You agree to use the Service lawfully and in a manner that does not harm others or disrupt the Service.
          You may not attempt to:
        </p>
        <ul className="list-disc pl-6">
          <li>Reverse engineer, scrape, or extract data beyond the public interfaces provided by the Service.</li>
          <li>Bypass security features or access restricted parts of the Service.</li>
          <li>Use the Service to violate any applicable laws or third-party terms.</li>
        </ul>

        <h2 className="text-2xl font-bold mt-8 mb-4">3. Facebook Page Linking</h2>
        <p>
          If you link a Facebook Page, UniEvent will request limited permissions (such as <code>pages_show_list</code> and
          <code>pages_read_engagement</code>) to synchronize publicly available page event data. We store Page metadata
          and access tokens in encrypted form for this purpose.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">4. Intellectual Property</h2>
        <p>
          All rights, title, and interest in the Service and its content, including design, software, and text, are
          owned by or licensed to UniEvent. You may not copy, reproduce, modify, or redistribute content from the
          Service without explicit permission.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">5. Disclaimers</h2>
        <p>
          The Service is provided “AS IS” and “AS AVAILABLE.” UniEvent makes no warranties of any kind, whether express
          or implied, regarding the Service’s accuracy, availability, or fitness for any particular purpose.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">6. Limitation of Liability</h2>
        <p>
          To the fullest extent permitted by law, UniEvent will not be liable for indirect, incidental, special,
          consequential, or punitive damages arising out of or related to your use of the Service, including issues
          caused by third-party services or API downtime.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">7. Third‑Party Services</h2>
        <p>
          The Service depends on third-party providers such as Meta/Facebook APIs.
          Your use of the Service may also be subject to those third parties’ terms and policies.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">8. Data Deletion (Facebook/Meta)</h2>
        <p>
          If you have linked a Facebook Page and wish to delete the associated data, you may revoke access via
          Facebook Settings → Business Integrations, or email <strong>philippzhuravlev@gmail.com</strong> with the Page
          URL or ID. After verification, UniEvent will delete related tokens and synced data within a reasonable timeframe.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">9. Changes to Terms</h2>
        <p>
          We may modify these Terms from time to time. The “Last Updated” date at the top will reflect the latest
          version. Continued use of the Service after changes are posted constitutes acceptance of the updated Terms.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">10. Governing Law</h2>
        <p>
          These Terms are governed by the laws of Denmark in the municipality of Lyngby-Taarbæk.
        </p>

        <h2 className="text-2xl font-bold mt-8 mb-4">11. Contact</h2>
        <p>
          For questions about these Terms or the Service:<br />
          Email: <a href="mailto:philippzhuravlev@gmail.com" className="text-[var(--link-primary)] hover:underline">philippzhuravlev@gmail.com</a><br />
          GitHub: <a href="https://github.com/philippzhuravlev/UniEvent" className="text-[var(--link-primary)] hover:underline">github.com/philippzhuravlev/UniEvent</a>
        </p>
      </div>
    </div>
  );
}
