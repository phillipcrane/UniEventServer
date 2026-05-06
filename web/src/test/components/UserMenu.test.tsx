import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { UserMenu } from '../../components/UserMenu';

function renderMenu(onSignOut = vi.fn(), isSigningOut = false) {
    render(
        <MemoryRouter>
            <UserMenu userLabel="alice@example.com" onSignOut={onSignOut} isSigningOut={isSigningOut} />
        </MemoryRouter>,
    );
    return onSignOut;
}

describe('UserMenu', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('opens and closes the account menu', () => {
        renderMenu();

        fireEvent.click(screen.getByRole('button', { name: 'Open account menu' }));
        expect(screen.getByText('Signed in as')).toBeInTheDocument();
        expect(screen.getByText('alice@example.com')).toBeInTheDocument();

        fireEvent.mouseDown(document.body);
        expect(screen.queryByText('Signed in as')).not.toBeInTheDocument();
    });

    it('links to profile and closes after profile click', () => {
        renderMenu();

        fireEvent.click(screen.getByRole('button', { name: 'Open account menu' }));
        const profileLink = screen.getByRole('link', { name: /Profile/i });
        expect(profileLink).toHaveAttribute('href', '/profile');

        fireEvent.click(profileLink);

        expect(screen.queryByText('Signed in as')).not.toBeInTheDocument();
    });

    it('calls sign out and reflects signing-out state', () => {
        const onSignOut = renderMenu(vi.fn(), true);

        fireEvent.click(screen.getByRole('button', { name: 'Open account menu' }));
        const button = screen.getByRole('button', { name: /Signing out/i });

        expect(button).toBeDisabled();
        fireEvent.click(button);
        expect(onSignOut).not.toHaveBeenCalled();
    });

    it('calls sign out when enabled', () => {
        const onSignOut = renderMenu();

        fireEvent.click(screen.getByRole('button', { name: 'Open account menu' }));
        fireEvent.click(screen.getByRole('button', { name: /Log out/i }));

        expect(onSignOut).toHaveBeenCalledTimes(1);
    });
});
