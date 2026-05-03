import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { SignupPage } from '../../pages/SignupPage';

// These are fake functions we control in tests.
const mockNavigate = vi.fn();
const mockSignupWithEmail = vi.fn();
const mockMapAuthError = vi.fn();
const mockVerifyOrganizerKey = vi.fn();

// We replace real navigation with our fake one, so no page actually changes.
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('../../services/auth', () => ({
    signupWithEmail: (...args: unknown[]) => mockSignupWithEmail(...args),
}));

vi.mock('../../utils/authUtils', () => ({
    mapAuthError: (...args: unknown[]) => mockMapAuthError(...args),
    createHttpError: (status: number, message: string) => Object.assign(new Error(message), { status }),
    isValidEmail: (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v),
}));

vi.mock('../../services/dal', () => ({
    verifyOrganizerKey: (...args: unknown[]) => mockVerifyOrganizerKey(...args),
}));

// Small helper to open the page in a test-safe router.
function renderPage() {
    return render(
        <MemoryRouter>
            <SignupPage />
        </MemoryRouter>
    );
}

async function chooseUserRole(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByRole('button', { name: /User/i }));
}

async function chooseOrganizerRole(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByRole('button', { name: /Organizer/i }));
}

describe('SignupPage', () => {
    beforeEach(() => {
        // Clean state before each test so tests do not affect each other.
        mockNavigate.mockReset();
        mockSignupWithEmail.mockReset();
        mockMapAuthError.mockReset();
        mockVerifyOrganizerKey.mockReset();
    });

    it('shows an error when fields are empty', async () => {
        // Simple check: all fields are required.
        const user = userEvent.setup();
        renderPage();

        await chooseUserRole(user);
        await user.click(screen.getByRole('button', { name: 'Sign Up as User' }));

        expect(screen.getByText('Please fill in all fields.')).toBeInTheDocument();
        expect(mockSignupWithEmail).not.toHaveBeenCalled();
    });

    it('opens role modal before signup', async () => {
        renderPage();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText('Do you want to sign up as User or Organizer?')).toBeInTheDocument();
        expect(mockSignupWithEmail).not.toHaveBeenCalled();
    });

    it('navigates to login when clicking outside role modal', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('dialog'));

        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true });
    });

    it('does not navigate when clicking inside role modal content', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByText('Do you want to sign up as User or Organizer?'));

        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('shows an error when password and confirm password differ', async () => {
        // Simple check: both password fields must match.
        const user = userEvent.setup();
        renderPage();

        await user.type(screen.getByLabelText('Username'), 'alice');
        await user.type(screen.getByLabelText('Email'), 'alice@example.com');
        await user.type(screen.getByLabelText('Password'), '123456789012');
        await user.type(screen.getByLabelText('Confirm Password'), '210987654321');
        await chooseUserRole(user);
        await user.click(screen.getByRole('button', { name: 'Sign Up as User' }));

        expect(screen.getByText('Passwords do not match.')).toBeInTheDocument();
        expect(mockSignupWithEmail).not.toHaveBeenCalled();
    });

    it('submits valid signup data and navigates to home', async () => {
        // Happy path: valid data should create account and continue.
        const user = userEvent.setup();
        mockSignupWithEmail.mockResolvedValueOnce({ uid: 'new-user' });
        renderPage();

        await user.type(screen.getByLabelText('Username'), 'alice');
        await user.type(screen.getByLabelText('Email'), 'alice@example.com');
        await user.type(screen.getByLabelText('Password'), '123456789012');
        await user.type(screen.getByLabelText('Confirm Password'), '123456789012');
        await chooseUserRole(user);
        await user.click(screen.getByRole('button', { name: 'Sign Up as User' }));

        expect(mockSignupWithEmail).toHaveBeenCalledWith({
            username: 'alice',
            email: 'alice@example.com',
            password: '123456789012',
            role: 'user',
            confirmationToken: undefined,
        });
        expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
    });

    it('shows mapped backend error when signup fails', async () => {
        // If backend rejects signup, user should still get a clear message.
        const user = userEvent.setup();
        const error = new Error('signup-fail');
        mockSignupWithEmail.mockRejectedValueOnce(error);
        mockMapAuthError.mockReturnValueOnce('This email is already in use.');
        renderPage();

        await user.type(screen.getByLabelText('Username'), 'alice');
        await user.type(screen.getByLabelText('Email'), 'alice@example.com');
        await user.type(screen.getByLabelText('Password'), '123456789012');
        await user.type(screen.getByLabelText('Confirm Password'), '123456789012');
        await chooseUserRole(user);
        await user.click(screen.getByRole('button', { name: 'Sign Up as User' }));

        expect(mockMapAuthError).toHaveBeenCalledWith(error);
        expect(screen.getByText('This email is already in use.')).toBeInTheDocument();
    });

    it('requires organizer access password when role is Organizer', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.type(screen.getByLabelText('Username'), 'org-admin');
        await user.type(screen.getByLabelText('Email'), 'org@example.com');
        await user.type(screen.getByLabelText('Password'), '123456789012');
        await user.type(screen.getByLabelText('Confirm Password'), '123456789012');

        await chooseOrganizerRole(user);
        await user.click(screen.getByRole('button', { name: 'Sign Up as Organizer' }));

        expect(screen.getByText('Please enter your organizer invitation key.')).toBeInTheDocument();
        expect(mockSignupWithEmail).not.toHaveBeenCalled();
    });

    it('verifies organizer key via API and passes it to signup', async () => {
        // Keys are validated server-side now, no hardcoded codes in the bundle.
        const user = userEvent.setup();
        mockSignupWithEmail.mockResolvedValueOnce({ uid: 'new-organizer-user' });
        mockVerifyOrganizerKey.mockResolvedValue({ confirmationToken: 'confirm-token', expiresIn: 600, email: 'org@example.com' });
        renderPage();

        await user.type(screen.getByLabelText('Username'), 'org-admin');
        await user.type(screen.getByLabelText('Email'), 'org@example.com');
        await user.type(screen.getByLabelText('Password'), '123456789012');
        await user.type(screen.getByLabelText('Confirm Password'), '123456789012');

        await chooseOrganizerRole(user);
        await user.type(screen.getByLabelText('Invitation Key'), 'my-invite-key');
        await user.click(screen.getByRole('button', { name: 'Sign Up as Organizer' }));

        expect(mockVerifyOrganizerKey).toHaveBeenCalledWith('my-invite-key');
        expect(mockSignupWithEmail).toHaveBeenCalledWith({
            username: 'org-admin',
            email: 'org@example.com',
            password: '123456789012',
            role: 'organizer',
            confirmationToken: 'confirm-token',
        });
        expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
    });

    it('shows error and does not sign up when organizer key is rejected by server', async () => {
        const user = userEvent.setup();
        mockVerifyOrganizerKey.mockResolvedValue(null);
        renderPage();

        await user.type(screen.getByLabelText('Username'), 'org-admin');
        await user.type(screen.getByLabelText('Email'), 'org@example.com');
        await user.type(screen.getByLabelText('Password'), '123456789012');
        await user.type(screen.getByLabelText('Confirm Password'), '123456789012');

        await chooseOrganizerRole(user);
        await user.type(screen.getByLabelText('Invitation Key'), 'bad-key');
        await user.click(screen.getByRole('button', { name: 'Sign Up as Organizer' }));

        expect(screen.getByText('Organizer invitation key is invalid.')).toBeInTheDocument();
        expect(mockSignupWithEmail).not.toHaveBeenCalled();
    });
});
