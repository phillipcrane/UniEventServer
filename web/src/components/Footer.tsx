import { Link } from 'react-router-dom';
import { Github, Mail } from 'lucide-react';

export function Footer() {
  return (
    <footer className="mt-12 bg-[var(--dtu-secondary-bg)] border-t border-[var(--panel-border)] py-8">
      <div className="max-w-6xl mx-auto px-4 md:px-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Brand */}
          <div>
            <h3 className="font-bold text-lg text-[var(--text-primary)] mb-2">DTUEvent</h3>
            <p className="text-sm text-[var(--text-subtle)]">
              A unified calendar for DTU campus events
            </p>
          </div>

          {/* Legal */}
          <div>
            <h4 className="font-semibold text-[var(--text-primary)] mb-3">Legal</h4>
            <ul className="space-y-2 text-sm">
              <li>
                <Link to="/terms" className="text-[var(--link-primary)] hover:text-[var(--link-primary-hover)] transition-colors">
                  Terms and Conditions
                </Link>
              </li>
              <li>
                <Link to="/privacy" className="text-[var(--link-primary)] hover:text-[var(--link-primary-hover)] transition-colors">
                  Privacy Policy
                </Link>
              </li>
              <li>
                <Link
                  to="/data-deletion"
                  className="text-[var(--link-primary)] hover:text-[var(--link-primary-hover)] transition-colors"
                >
                  Data Deletion Instructions
                </Link>
              </li>
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h4 className="font-semibold text-[var(--text-primary)] mb-3">Contact</h4>
            <ul className="space-y-2 text-sm">
              <li>
                <a 
                  href="mailto:philippzhuravlev@gmail.com" 
                  className="text-[var(--link-primary)] hover:text-[var(--link-primary-hover)] transition-colors flex items-center gap-2"
                >
                  <Mail className="w-4 h-4" />
                  Philipp
                </a>
              </li>
              <li>
                <a 
                  href="mailto:crillerhylle@gmail.com" 
                  className="text-[var(--link-primary)] hover:text-[var(--link-primary-hover)] transition-colors flex items-center gap-2"
                >
                  <Mail className="w-4 h-4" />
                  Christian
                </a>
              </li>
              <li>
                <a 
                  href="https://github.com/philippzhuravlev/DTUEvent" 
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-[var(--link-primary)] hover:text-[var(--link-primary-hover)] transition-colors flex items-center gap-2"
                >
                  <Github className="w-4 h-4" />
                  GitHub
                </a>
              </li>
            </ul>
          </div>
        </div>

        <hr className="my-6 border-[var(--panel-border)]" />

        <div className="text-center text-xs text-[var(--text-subtle)]">
          <p>&copy; 2025 DTUEvent. Made by TonkaProductions.</p>
          <p>We aggregate public Facebook events. DTUEvent is not affiliated with DTU, Meta, or Facebook.</p>
        </div>
      </div>
    </footer>
  );
}
