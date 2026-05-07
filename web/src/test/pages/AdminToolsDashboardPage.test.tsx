import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminToolsDashboardPage } from '../../pages/AdminToolsDashboardPage';

const mockUseAuth = vi.fn();
const mockLoadAdminPages = vi.fn();
const mockRefreshAllAdminTokens = vi.fn();
const mockRefreshAdminToken = vi.fn();
const mockIngestAdminPage = vi.fn();
const mockSeedAdminData = vi.fn();
const mockClearAdminData = vi.fn();

vi.mock('../../context/AuthContext', () => ({
    useAuth: () => mockUseAuth(),
}));

vi.mock('../../services/adminTools', () => ({
    loadAdminPages: () => mockLoadAdminPages(),
    refreshAllAdminTokens: () => mockRefreshAllAdminTokens(),
    refreshAdminToken: (pageId: string) => mockRefreshAdminToken(pageId),
    ingestAdminPage: (pageId: string) => mockIngestAdminPage(pageId),
    seedAdminData: () => mockSeedAdminData(),
    clearAdminData: () => mockClearAdminData(),
    mapAdminToolsError: (error: unknown) => (error instanceof Error ? error.message : 'Something went wrong'),
}));

function renderPage() {
    return render(
        <MemoryRouter>
            <AdminToolsDashboardPage />
        </MemoryRouter>,
    );
}

describe('AdminToolsDashboardPage', () => {
    beforeEach(() => {
        mockUseAuth.mockReturnValue({ currentUser: { role: 'admin', email: 'admin@example.com' } });
        mockLoadAdminPages.mockReset();
        mockRefreshAllAdminTokens.mockReset();
        mockRefreshAdminToken.mockReset();
        mockIngestAdminPage.mockReset();
        mockSeedAdminData.mockReset();
        mockClearAdminData.mockReset();
        mockLoadAdminPages.mockResolvedValue([
            { id: 'page-1', name: 'Main Page', tokenStatus: 'valid', tokenExpiresInDays: 12 },
        ]);
        mockRefreshAllAdminTokens.mockResolvedValue({ refreshedCount: 1, failedCount: 0, durationMs: 1200 });
        mockRefreshAdminToken.mockResolvedValue({ pageId: 'page-1', success: true, message: 'Token refreshed' });
        mockIngestAdminPage.mockResolvedValue({ pageId: 'page-1', eventCount: 3, eventTitles: ['A', 'B', 'C'] });
        mockSeedAdminData.mockResolvedValue({ success: true, message: 'Seeded', pageCount: 2, eventCount: 10, placeCount: 2 });
        mockClearAdminData.mockResolvedValue({ success: true, message: 'Cleared', pageCount: 0, eventCount: 0, placeCount: 0 });
    });

    it('shows the dashboard and loads pages', async () => {
        renderPage();

        expect(screen.getByRole('heading', { name: 'Admin Dashboard' })).toBeInTheDocument();
        expect(await screen.findByText('Main Page')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Refresh token' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Ingest events' })).toBeInTheDocument();
    });

    it('runs the refresh-all action', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Refresh all tokens' }));

        await waitFor(() => expect(mockRefreshAllAdminTokens).toHaveBeenCalled());
        expect(await screen.findByText(/1 refreshed, 0 failed in 1200ms/)).toBeInTheDocument();
    });
});