import { Link } from 'react-router-dom';
import { HeaderLogoLink } from '../HeaderLogoLink';

type AdminHeaderProps = {
    title: string;
    subtitle: string;
};

export function AdminHeader({ title, subtitle }: AdminHeaderProps) {
    return (
        <header className="page-header mx-6 md:mx-8 mt-4 md:mt-6 mb-8">
            <div className="header-content">
                <HeaderLogoLink />
                <div className="header-text profile-header-text">
                    <h1 className="header-title">{title}</h1>
                    <p className="header-subtitle">{subtitle}</p>
                </div>
            </div>

            <div className="header-toggle relative flex items-center gap-2 rounded-xl border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-2 shadow-sm">
                <Link
                    to="/profile"
                    className="inline-flex items-center justify-center rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-3 py-1.5 text-sm font-semibold text-[var(--text-primary)] transition-colors duration-200 hover:bg-[var(--button-hover)]"
                >
                    Back to Profile
                </Link>
            </div>
        </header>
    );
}