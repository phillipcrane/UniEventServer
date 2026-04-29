import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { LoginPage } from '../../pages/LoginPage';

// These are fake functions we control in tests.
const mockNavigate = vi.fn();
const mockLoginWithEmail = vi.fn();
const mockMapAuthError = vi.fn();

// We replace real navigation with our fake one, so no page actually changes.
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('../../services/auth', () => ({
    loginWithEmail: (...args: unknown[]) => mockLoginWithEmail(...args),
}));

vi.mock('../../utils/authUtils', () => ({
    mapAuthError: (...args: unknown[]) => mockMapAuthError(...args),
    createHttpError: (status: number, message: string) => Object.assign(new Error(message), { status }),
    isValidEmail: (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v),
}));

// Small helper to open the page in a test-safe router.
function renderPage() {
    return render(
        <MemoryRouter>
            <LoginPage />
        </MemoryRouter>
    );
}

describe('LoginPage', () => {
    beforeEach(() => {
        // Clean state before each test so tests do not affect each other.
        mockNavigate.mockReset();
        mockLoginWithEmail.mockReset();
        mockMapAuthError.mockReset();
    });

    it('shows an error when fields are empty', async () => {
        // Simple check: user clicks sign in without typing anything.
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Sign In' }));

        expect(screen.getByText('Please provide both email and password.')).toBeInTheDocument();
        expect(mockLoginWithEmail).not.toHaveBeenCalled();
    });

    it('shows an error when email format is invalid', async () => {
        // Simple check: email must look like a real address.
        const user = userEvent.setup();
        renderPage();

        await user.type(screen.getByLabelText('Email'), 'invalid-email');
        await user.type(screen.getByLabelText('Password'), '123456');
        await user.click(screen.getByRole('button', { name: 'Sign In' }));

        expect(screen.getByText('Please provide a valid email address.')).toBeInTheDocument();
        expect(mockLoginWithEmail).not.toHaveBeenCalled();
    });

    it('submits valid credentials and navigates to home', async () => {
        // Happy path: valid input should call login and continue.
        const user = userEvent.setup();
        mockLoginWithEmail.mockResolvedValueOnce({ uid: 'abc' });
        renderPage();

        await user.type(screen.getByLabelText('Email'), 'user@example.com');
        await user.type(screen.getByLabelText('Password'), '123456');
        await user.click(screen.getByRole('button', { name: 'Sign In' }));

        expect(mockLoginWithEmail).toHaveBeenCalledWith('user@example.com', '123456');
        expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
    });

    it('shows mapped backend error when login fails', async () => {
        // If backend rejects, we show a friendly message to the user.
        const user = userEvent.setup();
        const error = new Error('backend-fail');
        mockLoginWithEmail.mockRejectedValueOnce(error);
        mockMapAuthError.mockReturnValueOnce('Invalid email or password.');
        renderPage();

        await user.type(screen.getByLabelText('Email'), 'user@example.com');
        await user.type(screen.getByLabelText('Password'), 'wrongpass');
        await user.click(screen.getByRole('button', { name: 'Sign In' }));

        expect(mockMapAuthError).toHaveBeenCalledWith(error);
        expect(screen.getByText('Invalid email or password.')).toBeInTheDocument();
    });
});
