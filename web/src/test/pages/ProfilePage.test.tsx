import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ProfilePage } from '../../pages/ProfilePage';
import type { Event as EventType, AccountRole } from '../../types';

const mockUseProfilePage = vi.fn();

vi.mock('../../hooks/useProfilePage', () => ({
    useProfilePage: () => mockUseProfilePage(),
}));

// LikeButton (inside SavedEventCard) uses both contexts.
vi.mock('../../context/AuthContext', () => ({
    useAuth: () => ({ currentUser: null }),
}));
vi.mock('../../context/LikesContext', () => ({
    useLikes: () => ({ likedIds: new Set(), isLiked: () => false, toggle: async () => false }),
}));

function makeEvent(id: string, title: string): EventType {
    return {
        id,
        title,
        pageId: 'p1',
        startTime: new Date(Date.now() + 86400000).toISOString(),
        description: '',
        coverImageUrl: undefined,
    } as EventType;
}

function defaultHookReturn(overrides: Partial<ReturnType<typeof mockUseProfilePage>> = {}) {
    return {
        currentUser: { uid: 'u1', email: 'alice@example.com', role: 'user', username: 'alice' } as never,
        accountRole: 'user' as AccountRole,
        organizerNames: [],
        isSigningOut: false,
        fbConnecting: false,
        fbError: null,
        isLoadingLikedEvents: false,
        likedEvents: [],
        userLabel: 'alice@example.com',
        username: 'alice',
        profileImage: null,
        handleFacebookConnect: vi.fn(),
        handleSignOut: vi.fn(),
        ...overrides,
    };
}

function renderPage() {
    return render(
        <MemoryRouter>
            <ProfilePage />
        </MemoryRouter>
    );
}

describe('ProfilePage', () => {
    beforeEach(() => {
        mockUseProfilePage.mockReset();
        mockUseProfilePage.mockReturnValue(defaultHookReturn());
    });

    it('shows the username', () => {
        renderPage();

        expect(screen.getByRole('heading', { name: 'alice' })).toBeInTheDocument();
    });

    it('shows "User" role badge for regular users', () => {
        renderPage();

        expect(screen.getByText('User')).toBeInTheDocument();
    });

    it('shows "Organizer" role badge for organizer accounts', () => {
        mockUseProfilePage.mockReturnValue(defaultHookReturn({ accountRole: 'organizer' }));
        renderPage();

        expect(screen.getByText('Organizer')).toBeInTheDocument();
    });

    it('shows "Admin" role badge for admin accounts', () => {
        mockUseProfilePage.mockReturnValue(defaultHookReturn({ accountRole: 'admin' }));
        renderPage();

        expect(screen.getByText('Admin')).toBeInTheDocument();
    });

    it('shows the user email', () => {
        renderPage();

        expect(screen.getAllByText('alice@example.com').length).toBeGreaterThan(0);
    });

    it('shows Facebook connect section for organizer role', () => {
        mockUseProfilePage.mockReturnValue(defaultHookReturn({ accountRole: 'organizer' }));
        renderPage();

        expect(screen.getByRole('button', { name: 'Connect Facebook Page' })).toBeInTheDocument();
    });

    it('shows Facebook connect section for admin role', () => {
        mockUseProfilePage.mockReturnValue(defaultHookReturn({ accountRole: 'admin' }));
        renderPage();

        expect(screen.getByRole('button', { name: 'Connect Facebook Page' })).toBeInTheDocument();
    });

    it('shows admin dashboard link for admin accounts', () => {
        mockUseProfilePage.mockReturnValue(defaultHookReturn({ accountRole: 'admin' }));
        renderPage();

        expect(screen.getByRole('link', { name: 'Admin Dashboard' }))
            .toHaveAttribute('href', '/admin');
    });

    it('hides Facebook connect section for regular user', () => {
        renderPage();

        expect(screen.queryByRole('button', { name: 'Connect Facebook Page' })).not.toBeInTheDocument();
    });

    it('calls handleFacebookConnect when the connect button is clicked', async () => {
        const user = userEvent.setup();
        const handleFacebookConnect = vi.fn();
        mockUseProfilePage.mockReturnValue(defaultHookReturn({
            accountRole: 'organizer',
            handleFacebookConnect,
        }));
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Connect Facebook Page' }));

        expect(handleFacebookConnect).toHaveBeenCalled();
    });

    it('disables Facebook connect while connection is starting', () => {
        mockUseProfilePage.mockReturnValue(defaultHookReturn({
            accountRole: 'organizer',
            fbConnecting: true,
        }));
        renderPage();

        expect(screen.getByRole('button', { name: 'Connecting...' })).toBeDisabled();
    });

    it('shows empty saved events state when no events are liked', () => {
        renderPage();

        expect(screen.getByText('No liked events yet')).toBeInTheDocument();
    });

    it('shows loading state while liked events are being fetched', () => {
        mockUseProfilePage.mockReturnValue(defaultHookReturn({ isLoadingLikedEvents: true }));
        renderPage();

        expect(screen.getByText('Loading liked events...')).toBeInTheDocument();
    });

    it('renders saved event cards when liked events are present', () => {
        const events = [makeEvent('e1', 'Concert Night'), makeEvent('e2', 'Hackathon')];
        mockUseProfilePage.mockReturnValue(defaultHookReturn({ likedEvents: events }));
        renderPage();

        expect(screen.getByText('Concert Night')).toBeInTheDocument();
        expect(screen.getByText('Hackathon')).toBeInTheDocument();
    });

    it('shows saved event count badge', () => {
        const events = [makeEvent('e1', 'Concert Night')];
        mockUseProfilePage.mockReturnValue(defaultHookReturn({ likedEvents: events }));
        renderPage();

        // Two "1 saved" badges (profile overview + saved events header)
        expect(screen.getAllByText('1 saved').length).toBeGreaterThan(0);
    });

    it('shows Facebook error when present', () => {
        mockUseProfilePage.mockReturnValue(defaultHookReturn({
            accountRole: 'organizer',
            fbError: 'Could not connect Facebook.',
        }));
        renderPage();

        expect(screen.getByRole('alert')).toHaveTextContent('Could not connect Facebook.');
    });

    it('calls handleSignOut when logout button is clicked', async () => {
        const user = userEvent.setup();
        const handleSignOut = vi.fn();
        mockUseProfilePage.mockReturnValue(defaultHookReturn({ handleSignOut }));
        renderPage();

        await user.click(screen.getByRole('button', { name: /log out/i }));

        expect(handleSignOut).toHaveBeenCalled();
    });

    it('disables logout button while sign out is in progress', () => {
        mockUseProfilePage.mockReturnValue(defaultHookReturn({ isSigningOut: true }));
        renderPage();

        expect(screen.getByRole('button', { name: /log out/i })).toBeDisabled();
    });

    it('shows connected organizer names when present', () => {
        mockUseProfilePage.mockReturnValue(defaultHookReturn({
            accountRole: 'organizer',
            organizerNames: ['DTU Events', 'Tech Society'],
        }));
        renderPage();

        expect(screen.getByText(/DTU Events, Tech Society/)).toBeInTheDocument();
    });
});
