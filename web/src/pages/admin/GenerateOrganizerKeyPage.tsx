import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Footer } from '../../components/Footer';
import { AdminHeader } from '../../components/admin/AdminHeader';
import { useAuth } from '../../context/AuthContext';
import {
    generateOrganizerKey,
    mapAdminKeyError,
} from '../../services/auth';
import { sanitizeErrorMessage } from '../../utils/securityUtils';
import { isValidEmail, isValidEmailLength } from '../../utils/validationUtils';

function formatExpiryHours(expiresIn: number | null): string {
    if (!expiresIn || expiresIn <= 0) {
        return 'Valid for: unknown duration';
    }

    return `Valid for: ${Math.round(expiresIn / 3600)} hours`;
}

export function GenerateOrganizerKeyPage() {
    const navigate = useNavigate();
    const { currentUser } = useAuth();

    const [isAuthorizing, setIsAuthorizing] = useState(true);
    const [isAdmin, setIsAdmin] = useState(false);

    const [email, setEmail] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [emailError, setEmailError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const [keyExpiresIn, setKeyExpiresIn] = useState<number | null>(null);

    const isSubmitDisabled = useMemo(
        () => isSubmitting || !email.trim(),
        [email, isSubmitting],
    );

    useEffect(() => {
        if (!currentUser) {
            navigate('/login', { replace: true });
            return;
        }

        if (currentUser.role !== 'admin') {
            navigate('/', { replace: true });
            return;
        }

        setIsAdmin(true);
        setIsAuthorizing(false);
    }, [currentUser, navigate]);

    function validateForm(): boolean {
        const trimmedEmail = email.trim();

        if (!trimmedEmail) {
            setEmailError('Email is required.');
            return false;
        }

        if (!isValidEmail(trimmedEmail)) {
            setEmailError('Please enter a valid email address.');
            return false;
        }

        if (!isValidEmailLength(trimmedEmail)) {
            setEmailError('Email is too long.');
            return false;
        }

        setEmailError('');
        return true;
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        setErrorMessage('');
        setSuccessMessage('');

        if (!validateForm()) {
            return;
        }

        setIsSubmitting(true);

        try {
            const result = await generateOrganizerKey({ email: email.trim() });
            setSuccessMessage(sanitizeErrorMessage(result.message));
            setKeyExpiresIn(result.expiresIn);
            setEmail('');
            setEmailError('');
        } catch (error) {
            const mappedError = mapAdminKeyError(error);
            setErrorMessage(sanitizeErrorMessage(mappedError));

            if (error && typeof error === 'object' && 'status' in error) {
                const status = (error as { status?: number }).status;
                if (status === 401) {
                    setTimeout(() => navigate('/login', { replace: true }), 2000);
                }
                if (status === 403) {
                    setTimeout(() => navigate('/', { replace: true }), 3000);
                }
            }
        } finally {
            setIsSubmitting(false);
        }
    }

    function handleGenerateAnother() {
        setSuccessMessage('');
        setKeyExpiresIn(null);
        setErrorMessage('');
    }

    if (isAuthorizing) {
        return (
            <div className="min-h-screen flex flex-col">
                <AdminHeader
                    title="Generate Organizer Key"
                    subtitle="Invite new organizers to join the platform"
                />
                <main className="flex-1 px-6 md:px-8 pb-12 max-w-3xl mx-auto w-full">
                    <div className="rounded-2xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 shadow-lg">
                        <p className="text-sm text-[var(--text-subtle)]">Checking admin access...</p>
                    </div>
                </main>
                <Footer />
            </div>
        );
    }

    if (!currentUser || !isAdmin) {
        return null;
    }

    return (
        <div className="min-h-screen flex flex-col">
            <AdminHeader
                title="Generate Organizer Key"
                subtitle="Invite new organizers to join the platform"
            />

            <main className="flex-1 px-6 md:px-8 pb-12 max-w-3xl mx-auto w-full">
                <section className="rounded-2xl border border-[var(--panel-border)] bg-[var(--panel-bg)] p-6 shadow-lg">
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[var(--text-subtle)]">
                        Admin Action
                    </p>
                    <h2 className="mt-1 text-2xl font-bold text-[var(--text-primary)]">
                        Send organizer invitation
                    </h2>
                    <p className="mt-2 text-sm text-[var(--text-subtle)]">
                        The invitation key is sent directly by email and is valid for 24 hours.
                    </p>

                    <form className="mt-6 space-y-4" noValidate onSubmit={handleSubmit}>
                        <div>
                            <label
                                htmlFor="organizer-email"
                                className="block text-sm font-semibold text-[var(--text-primary)]"
                            >
                                Organizer Email
                            </label>
                            <input
                                id="organizer-email"
                                type="email"
                                placeholder="organizer@example.com"
                                value={email}
                                onChange={(event) => {
                                    setEmail(event.target.value);
                                    setEmailError('');
                                }}
                                onBlur={() => {
                                    if (!email.trim()) {
                                        setEmailError('Email is required.');
                                        return;
                                    }

                                    if (!isValidEmail(email.trim())) {
                                        setEmailError('Please enter a valid email address.');
                                        return;
                                    }

                                    if (!isValidEmailLength(email.trim())) {
                                        setEmailError('Email is too long.');
                                        return;
                                    }

                                    setEmailError('');
                                }}
                                disabled={isSubmitting}
                                className="mt-2 w-full rounded-lg border border-[var(--panel-border)] bg-[var(--input-bg)] px-4 py-3 text-sm text-[var(--text-primary)] outline-none transition focus:border-[var(--input-focus-border)]"
                                aria-invalid={Boolean(emailError)}
                                aria-describedby={emailError ? 'organizer-email-error' : undefined}
                            />
                            {emailError && (
                                <span
                                    id="organizer-email-error"
                                    role="alert"
                                    className="mt-2 block text-sm text-red-500"
                                >
                                    {emailError}
                                </span>
                            )}
                        </div>

                        {errorMessage && (
                            <div
                                className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-200"
                                role="alert"
                            >
                                {errorMessage}
                            </div>
                        )}

                        <button
                            type="submit"
                            disabled={isSubmitDisabled}
                            className="inline-flex items-center justify-center rounded-lg bg-[var(--link-primary)] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[var(--link-primary-hover)] disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {isSubmitting ? 'Generating...' : 'Generate & Send Invitation'}
                        </button>
                    </form>
                </section>

                {successMessage && (
                    <section
                        className="mt-6 rounded-2xl border border-emerald-400/35 bg-emerald-500/10 p-6 shadow-lg"
                        role="alert"
                    >
                        <h3 className="text-lg font-bold text-emerald-100">Success</h3>
                        <p className="mt-2 text-sm text-emerald-100/90">{successMessage}</p>
                        <p className="mt-2 text-xs font-semibold uppercase tracking-[0.08em] text-emerald-200/90">
                            {formatExpiryHours(keyExpiresIn)}
                        </p>
                        <button
                            type="button"
                            onClick={handleGenerateAnother}
                            className="mt-4 inline-flex items-center justify-center rounded-lg border border-[var(--panel-border)] bg-[var(--panel-bg)] px-4 py-2 text-sm font-semibold text-[var(--text-primary)] transition hover:bg-[var(--button-hover)]"
                        >
                            Generate Another
                        </button>
                    </section>
                )}
            </main>

            <Footer />
        </div>
    );
}