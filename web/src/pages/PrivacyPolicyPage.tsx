import { useNavigate } from 'react-router-dom';

export function PrivacyPolicyPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-black text-white">
      <div className="max-w-3xl mx-auto px-4 md:px-6 py-8 prose prose-invert">
        <button
          onClick={() => navigate('/')}
          className="mb-6 text-[var(--link-primary)] hover:underline font-semibold"
        >
          ← Back to Events
        </button>

        <h1 className="text-4xl font-bold mb-4">Privacy Policy</h1>
        <p className="text-[var(--text-subtle)]">
          <strong>Last Updated:</strong> February 12, 2026<br />
          <strong>Effective Date:</strong> February 12, 2026
        </p>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">1. Data Controller</h2>
          <p>
            UniEvent is operated by <strong>Philipp Zhuravlev</strong>, who is responsible for how personal data is
            collected and used in connection with this Service. For questions about this Privacy Policy or personal
            data, contact <a href="mailto:philippzhuravlev@gmail.com">philippzhuravlev@gmail.com</a>.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">2. Information We Do Not Intentionally Collect</h2>
          <p>
            UniEvent does not intentionally collect personal data beyond what is needed to operate the Service. We do
            not use advertising trackers or marketing analytics. If you choose to register an account, you provide a
            username and email address, which are stored to support your account. Anonymous visitors (not signed in)
            are not identified or profiled.
          </p>
          <p>
            Technical logs may be generated automatically by hosting providers (for example timestamps or error logs)
            for security and reliability, but these are not used to identify or profile individuals.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">3. Information We Collect to Operate the Service</h2>
          <p>
            UniEvent stores publicly available event information necessary to display event listings. This may include
            publicly accessible event titles, descriptions, dates/times, locations, images, URLs, and aggregated metrics.
          </p>
          <p>
            If a Facebook Page administrator connects a Page to UniEvent, we store Page metadata (Page ID, name, and
            URL) and a Page access token (encrypted) in order to keep event data synchronized. We do not store your
            Facebook profile identity (such as your user ID, name, email address, or profile photo).
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">4. How We Use Your Data</h2>
          <p>
            We use information only to operate, secure, and maintain the Service, display public events, and keep
            connected Pages in sync. We do not use collected data for advertising or profiling outside the core
            functionality of the Service. This policy is provided to meet legal transparency obligations under data
            protection laws.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">5. Cookies & Tracking Technologies</h2>
          <p>
            UniEvent does not intentionally set cookies for advertising or cross‑site tracking. However, third‑party
            infrastructure services may set essential technical cookies outside of our control that are necessary for
            basic technical functions.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">6. Data Retention</h2>
          <p>
            Event data is kept only as long as necessary to provide the Service and for a limited period after relevant
            events have passed. Access tokens and Page metadata are kept only while a Page remains connected and,
            after verified deletion requests, are deleted within a reasonable timeframe.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">7. Data Deletion</h2>
          <p>
            Facebook Page administrators may revoke UniEvent’s access via Facebook Settings &gt; Business Integrations or
            by sending a verified deletion request to <a href="mailto:philippzhuravlev@gmail.com">philippzhuravlev@gmail.com</a> with the
            Page ID or URL. Upon verified request, we will delete stored tokens and related Page data.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">8. Your Rights</h2>
          <p>
            Under applicable data protection laws such as GDPR, individuals may have the right to:
          </p>
          <ul className="list-disc pl-6">
            <li>Access the personal data we hold about you</li>
            <li>Request correction or deletion</li>
            <li>Restrict or object to processing</li>
            <li>Request data portability</li>
            <li>Lodge a complaint with the Danish Data Protection Authority (Datatilsynet)</li>
          </ul>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">9. Third‑Party Services</h2>
          <p>
            UniEvent uses Meta/Facebook APIs to fetch public event data, and self-hosted infrastructure (database and
            encrypted secrets storage) to operate the Service. Each third party’s privacy policy governs their
            respective processing activities.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">10. Security</h2>
          <p>
            We use reasonable technical measures such as encrypted storage for access tokens and HTTPS/TLS for data in
            transit to protect your data. No system can guarantee absolute security.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">11. Changes to This Policy</h2>
          <p>
            We may update this policy to reflect changes in our practices or legal requirements. The “Last Updated”
            date at the top will be revised accordingly.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-bold mt-8 mb-4">12. Contact</h2>
          <p>
            Questions about this Privacy Policy should be directed to:<br />
            Email: <a href="mailto:philippzhuravlev@gmail.com">philippzhuravlev@gmail.com</a><br />
            UniEvent GitHub: <a href="https://github.com/philippzhuravlev/UniEvent">github.com/philippzhuravlev/UniEvent</a>
          </p>
        </section>
      </div>
    </div>
  );
}
