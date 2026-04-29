import { useCallback, useEffect, useRef, useState } from 'react';
import { getEventById } from '../services/dal';
import { logout } from '../services/auth';
import { mapAuthError } from '../utils/authUtils';
import { getOrganizerName, getEventCoverImageUrl } from '../utils/eventUtils';
import { useClickOutside } from './useClickOutside';
import { useAuth } from '../context/AuthContext';
import { usePages } from '../context/PagesContext';
import { SAVE_FEEDBACK_MS } from '../constants';
import type { Event } from '../types';

export function useEventPage(id: string | undefined) {
    const { currentUser } = useAuth();
    const pages = usePages();
    const [event, setEvent] = useState<Event | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSigningOut, setIsSigningOut] = useState(false);
    const [showAddMenu, setShowAddMenu] = useState(false);
    const [saveFeedback, setSaveFeedback] = useState('');
    const addMenuRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        if (!id) return;
        const fetchData = async () => {
            setIsLoading(true);
            try {
                const fetchedEvent = await getEventById(id);
                setEvent(fetchedEvent);
            } finally {
                setIsLoading(false);
            }
        };
        fetchData();
    }, [id]);

    const handleCloseMenu = useCallback(() => setShowAddMenu(false), []);
    useClickOutside(addMenuRef, showAddMenu, handleCloseMenu);

    const handleLikeToggle = (isSaved: boolean) => {
        setSaveFeedback(isSaved ? 'Saved to your profile.' : 'Removed from saved events.');
        window.setTimeout(() => setSaveFeedback(''), SAVE_FEEDBACK_MS);
    };

    async function handleSignOut() {
        try {
            setIsSigningOut(true);
            await logout();
        } catch (error) {
            console.error(mapAuthError(error));
        } finally {
            setIsSigningOut(false);
        }
    }

    return {
        currentUser,
        event,
        isLoading,
        isSigningOut,
        showAddMenu,
        setShowAddMenu,
        saveFeedback,
        addMenuRef,
        handleLikeToggle,
        handleSignOut,
        userLabel: currentUser?.displayName || currentUser?.email || 'My Profile',
        organizerName: getOrganizerName(event, pages),
        coverImageUrl: getEventCoverImageUrl(event?.coverImageUrl),
    };
}
