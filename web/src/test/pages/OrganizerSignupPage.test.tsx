import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { OrganizerSignupPage } from '../../pages/OrganizerSignupPage';

const mockNavigate = vi.fn();
const mockVerifyOrganizerKey = vi.fn();
const mockSignupWithEmail = vi.fn();
const mockMapAuthError = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../services/dal', () => ({
    verifyOrganizerKey: (...args: unknown[]) => mockVerifyOrganizerKey(...args),
}));

vi.mock('../../services/auth', () => ({
    signupWithEmail: (...args: unknown[]) => mockSignupWithEmail(...args),
}));

vi.mock('../../utils/authUtils', () => ({
    mapAuthError: (...args: unknown[]) => mockMapAuthError(...args),
    isValidEmail: (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v),
}));

vi.mock('../../context/AuthContext', () => ({
    useAuth: () => ({ currentUser: null }),
}));

function renderPage() {
    return render(
        <MemoryRouter>
            <OrganizerSignupPage />
        </MemoryRouter>
    );
}

async function advanceToStep2(user: ReturnType<typeof userEvent.setup>) {
    mockVerifyOrganizerKey.mockResolvedValueOnce({
        confirmationToken: 'token-abc',
        email: 'organizer@example.com',
    });
    await user.type(screen.getByLabelText('Invitation Key'), 'VALID-KEY-32CHARS');
    await user.click(screen.getByRole('button', { name: 'Verify Key' }));
    await screen.findByText('STEP 2 OF 2');
}

describe('OrganizerSignupPage', () => {
    beforeEach(() => {
        mockNavigate.mockReset();
        mockVerifyOrganizerKey.mockReset();
        mockSignupWithEmail.mockReset();
        mockMapAuthError.mockReset();
    });

    it('shows step 1 by default', () => {
        renderPage();

        expect(screen.getByText('STEP 1 OF 2')).toBeInTheDocument();
        expect(screen.getByLabelText('Invitation Key')).toBeInTheDocument();
    });

    it('shows an error when key is empty on submit', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Verify Key' }));

        expect(screen.getByText('Key is required.')).toBeInTheDocument();
        expect(mockVerifyOrganizerKey).not.toHaveBeenCalled();
    });

    it('calls verifyOrganizerKey with trimmed input', async () => {
        const user = userEvent.setup();
        mockVerifyOrganizerKey.mockResolvedValueOnce({
            confirmationToken: 'tok',
            email: 'a@b.com',
        });
        renderPage();

        await user.type(screen.getByLabelText('Invitation Key'), '  MY-KEY  ');
        await user.click(screen.getByRole('button', { name: 'Verify Key' }));

        expect(mockVerifyOrganizerKey).toHaveBeenCalledWith('MY-KEY');
    });

    it('advances to step 2 when key verification succeeds', async () => {
        const user = userEvent.setup();
        renderPage();

        await advanceToStep2(user);

        expect(screen.getByText('STEP 2 OF 2')).toBeInTheDocument();
    });

    it('shows mapped error when key verification fails', async () => {
        const user = userEvent.setup();
        const error = new Error('bad key');
        mockVerifyOrganizerKey.mockRejectedValueOnce(error);
        mockMapAuthError.mockReturnValueOnce('Organizer key is invalid or expired.');
        renderPage();

        await user.type(screen.getByLabelText('Invitation Key'), 'BAD-KEY');
        await user.click(screen.getByRole('button', { name: 'Verify Key' }));

        await waitFor(() => {
            expect(screen.getByText('Organizer key is invalid or expired.')).toBeInTheDocument();
        });
    });

    it('shows error when verifyOrganizerKey returns null', async () => {
        const user = userEvent.setup();
        mockVerifyOrganizerKey.mockResolvedValueOnce(null);
        renderPage();

        await user.type(screen.getByLabelText('Invitation Key'), 'NULL-KEY');
        await user.click(screen.getByRole('button', { name: 'Verify Key' }));

        await waitFor(() => {
            expect(screen.getByText('Organizer access key is invalid.')).toBeInTheDocument();
        });
    });

    it('navigate to landing page when Cancel is clicked', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(mockNavigate).toHaveBeenCalledWith('/signup-organizer-landing');
    });

    it('shows registration form on step 2 with email pre-filled and disabled', async () => {
        const user = userEvent.setup();
        renderPage();

        await advanceToStep2(user);

        const emailInput = screen.getByLabelText('Email') as HTMLInputElement;
        expect(emailInput.value).toBe('organizer@example.com');
        expect(emailInput.disabled).toBe(true);
    });

    it('shows error when registration fields are empty', async () => {
        const user = userEvent.setup();
        renderPage();

        await advanceToStep2(user);
        await user.click(screen.getByRole('button', { name: 'Complete Registration' }));

        expect(screen.getByText('Please fill in all fields.')).toBeInTheDocument();
        expect(mockSignupWithEmail).not.toHaveBeenCalled();
    });

    it('shows error when password is too short', async () => {
        const user = userEvent.setup();
        renderPage();

        await advanceToStep2(user);
        await user.type(screen.getByLabelText('Username'), 'orguser');
        await user.type(screen.getByLabelText('Password'), 'short');
        await user.type(screen.getByLabelText('Confirm Password'), 'short');
        await user.click(screen.getByRole('button', { name: 'Complete Registration' }));

        expect(screen.getByText(/at least 12 characters/i)).toBeInTheDocument();
        expect(mockSignupWithEmail).not.toHaveBeenCalled();
    });

    it('shows error when passwords do not match', async () => {
        const user = userEvent.setup();
        renderPage();

        await advanceToStep2(user);
        await user.type(screen.getByLabelText('Username'), 'orguser');
        await user.type(screen.getByLabelText('Password'), 'SecurePassword1!');
        await user.type(screen.getByLabelText('Confirm Password'), 'DifferentPassword1!');
        await user.click(screen.getByRole('button', { name: 'Complete Registration' }));

        expect(screen.getByText('Passwords do not match.')).toBeInTheDocument();
        expect(mockSignupWithEmail).not.toHaveBeenCalled();
    });

    it('calls signupWithEmail with correct payload on valid submission', async () => {
        const user = userEvent.setup();
        mockSignupWithEmail.mockResolvedValueOnce({});
        renderPage();

        await advanceToStep2(user);
        await user.type(screen.getByLabelText('Username'), 'orguser');
        await user.type(screen.getByLabelText('Password'), 'SecurePassword1!');
        await user.type(screen.getByLabelText('Confirm Password'), 'SecurePassword1!');
        await user.click(screen.getByRole('button', { name: 'Complete Registration' }));

        await waitFor(() => {
            expect(mockSignupWithEmail).toHaveBeenCalledWith({
                username: 'orguser',
                email: 'organizer@example.com',
                password: 'SecurePassword1!',
                role: 'organizer',
                confirmationToken: 'token-abc',
            });
        });
    });

    it('shows success message after registration', async () => {
        const user = userEvent.setup();
        mockSignupWithEmail.mockResolvedValueOnce({});
        renderPage();

        await advanceToStep2(user);
        await user.type(screen.getByLabelText('Username'), 'orguser');
        await user.type(screen.getByLabelText('Password'), 'SecurePassword1!');
        await user.type(screen.getByLabelText('Confirm Password'), 'SecurePassword1!');
        await user.click(screen.getByRole('button', { name: 'Complete Registration' }));

        await waitFor(() => {
            expect(screen.getByText(/redirecting to login/i)).toBeInTheDocument();
        });
    });

    it('go back button returns to step 1', async () => {
        const user = userEvent.setup();
        renderPage();

        await advanceToStep2(user);
        await user.click(screen.getByRole('button', { name: /go back/i }));

        expect(screen.getByText('STEP 1 OF 2')).toBeInTheDocument();
    });
});
