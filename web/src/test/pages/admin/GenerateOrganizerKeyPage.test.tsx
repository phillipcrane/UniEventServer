import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GenerateOrganizerKeyPage } from '../../../pages/GenerateOrganizerKeyPage';
import type { AuthUser } from '../../../services/auth';

const mockNavigate = vi.fn();
const mockGenerateOrganizerKey = vi.fn();
const mockMapAdminKeyError = vi.fn();

const authState = vi.hoisted(() => ({
    currentUser: null as AuthUser | null,
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../../context/AuthContext', () => ({
    useAuth: () => ({ currentUser: authState.currentUser }),
}));

vi.mock('../../../services/auth', () => ({
    generateOrganizerKey: (...args: unknown[]) => mockGenerateOrganizerKey(...args),
    mapAdminKeyError: (...args: unknown[]) => mockMapAdminKeyError(...args),
}));

function renderPage() {
    return render(
        <MemoryRouter>
            <GenerateOrganizerKeyPage />
        </MemoryRouter>,
    );
}

function setAdminUser() {
    authState.currentUser = {
        uid: 'admin-1',
        username: 'admin',
        email: 'admin@example.com',
        role: 'admin',
    };
}

describe('GenerateOrganizerKeyPage', () => {
    beforeEach(() => {
        authState.currentUser = null;
        mockNavigate.mockReset();
        mockGenerateOrganizerKey.mockReset();
        mockMapAdminKeyError.mockReset();
        vi.useRealTimers();
    });

    it('redirects anonymous users to login', () => {
        renderPage();

        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true });
        expect(screen.queryByLabelText('Organizer Email')).not.toBeInTheDocument();
    });

    it('redirects non-admin users home', () => {
        authState.currentUser = {
            uid: 'user-1',
            username: 'alice',
            email: 'alice@example.com',
            role: 'user',
        };

        renderPage();

        expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
        expect(screen.queryByLabelText('Organizer Email')).not.toBeInTheDocument();
    });

    it('validates organizer email before generating a key', async () => {
        setAdminUser();
        renderPage();

        fireEvent.change(screen.getByLabelText('Organizer Email'), { target: { value: 'not-an-email' } });
        fireEvent.click(screen.getByRole('button', { name: /Generate & Send Invitation/i }));

        expect(await screen.findByText('Please enter a valid email address.')).toBeInTheDocument();
        expect(mockGenerateOrganizerKey).not.toHaveBeenCalled();
    });

    it('shows required validation when organizer email is cleared and blurred', async () => {
        setAdminUser();
        renderPage();

        fireEvent.change(screen.getByLabelText('Organizer Email'), { target: { value: 'organizer@example.com' } });
        fireEvent.change(screen.getByLabelText('Organizer Email'), { target: { value: '' } });
        fireEvent.blur(screen.getByLabelText('Organizer Email'));

        expect(await screen.findByText('Email is required.')).toBeInTheDocument();
        expect(mockGenerateOrganizerKey).not.toHaveBeenCalled();
    });

    it('generates an invitation for a trimmed email and shows sanitized success details', async () => {
        setAdminUser();
        mockGenerateOrganizerKey.mockResolvedValueOnce({
            message: '<strong>Invitation sent</strong>',
            expiresIn: 86400,
        });
        renderPage();

        fireEvent.change(screen.getByLabelText('Organizer Email'), {
            target: { value: '  organizer@example.com  ' },
        });
        fireEvent.click(screen.getByRole('button', { name: /Generate & Send Invitation/i }));

        await waitFor(() => {
            expect(mockGenerateOrganizerKey).toHaveBeenCalledWith({ email: 'organizer@example.com' });
        });
        expect(screen.getByRole('alert')).toHaveTextContent('&lt;strong&gt;Invitation sent&lt;/strong&gt;');
        expect(screen.getByText('Valid for: 24 hours')).toBeInTheDocument();
        expect(screen.getByLabelText('Organizer Email')).toHaveValue('');
    });

    it('clears success details when generating another invitation', async () => {
        setAdminUser();
        mockGenerateOrganizerKey.mockResolvedValueOnce({
            message: 'Invitation sent',
            expiresIn: 86400,
        });
        renderPage();

        fireEvent.change(screen.getByLabelText('Organizer Email'), {
            target: { value: 'organizer@example.com' },
        });
        fireEvent.click(screen.getByRole('button', { name: /Generate & Send Invitation/i }));

        expect(await screen.findByText('Invitation sent')).toBeInTheDocument();
        expect(screen.getByText('Valid for: 24 hours')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: 'Generate Another' }));

        expect(screen.queryByText('Invitation sent')).not.toBeInTheDocument();
        expect(screen.queryByText('Valid for: 24 hours')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Generate & Send Invitation/i })).toBeInTheDocument();
    });

    it('sanitizes mapped admin errors and redirects unauthenticated users after a delay', async () => {
        vi.useFakeTimers();
        setAdminUser();
        const error = Object.assign(new Error('unauthenticated'), { status: 401 });
        mockGenerateOrganizerKey.mockRejectedValueOnce(error);
        mockMapAdminKeyError.mockReturnValueOnce('<b>Please sign in again.</b>');
        renderPage();

        fireEvent.change(screen.getByLabelText('Organizer Email'), {
            target: { value: 'organizer@example.com' },
        });
        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: /Generate & Send Invitation/i }));
        });

        expect(screen.getByRole('alert')).toHaveTextContent('&lt;b&gt;Please sign in again.&lt;/b&gt;');

        act(() => {
            vi.advanceTimersByTime(2000);
        });
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true });
    });

    it('sanitizes mapped admin errors and redirects unauthorized users after a delay', async () => {
        vi.useFakeTimers();
        setAdminUser();
        const error = Object.assign(new Error('forbidden'), { status: 403 });
        mockGenerateOrganizerKey.mockRejectedValueOnce(error);
        mockMapAdminKeyError.mockReturnValueOnce('<script>alert(1)</script>Admin role required.');
        renderPage();

        fireEvent.change(screen.getByLabelText('Organizer Email'), {
            target: { value: 'organizer@example.com' },
        });
        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: /Generate & Send Invitation/i }));
        });

        expect(screen.getByRole('alert')).toHaveTextContent('&lt;script&gt;alert(1)&lt;/script&gt;Admin role required.');
        expect(document.querySelector('script')).toBeNull();

        act(() => {
            vi.advanceTimersByTime(3000);
        });
        expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
    });
});
