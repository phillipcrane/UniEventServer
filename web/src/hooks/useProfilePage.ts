import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getEvents } from '../services/dal';
import { getAccountProfile } from '../services/auth';
import { logout } from '../services/auth';
import { getFacebookAuthUrl } from '../services/facebook';
import { buildUsername, filterAndSortLikedEvents } from '../utils/profileUtils';
import { useAuth } from '../context/AuthContext';
import { useLikes } from '../context/LikesContext';
import type { AccountRole, Event as EventType } from '../types';

export function useProfilePage() {
    const navigate = useNavigate();
    const { currentUser } = useAuth();
    const { likedIds } = useLikes();
    const [accountRole, setAccountRole] = useState<AccountRole>(currentUser?.role ?? 'user');
    const [organizerNames, setOrganizerNames] = useState<string[]>([]);
    const [isSigningOut, setIsSigningOut] = useState(false);
    const [fbConnecting, setFbConnecting] = useState(false);
    const [fbError, setFbError] = useState<string | null>(null);
    const [allEvents, setAllEvents] = useState<EventType[]>([]);
    const [isLoadingLikedEvents, setIsLoadingLikedEvents] = useState(true);

    useEffect(() => {
        if (currentUser === null) {
            navigate('/login', { replace: true });
        }
    }, [currentUser, navigate]);

    useEffect(() => {
        setAccountRole(currentUser?.role ?? 'user');
        setOrganizerNames(currentUser?.organizerNames ?? []);
    }, [currentUser?.role, currentUser?.organizerNames]);

    useEffect(() => {
        let cancelled = false;
        const loadAccountProfile = async () => {
            try {
                const profile = await getAccountProfile(currentUser?.uid);
                if (cancelled) return;
                setAccountRole(profile.role);
                setOrganizerNames(profile.organizerNames);
            } catch {
                if (cancelled) return;
                setAccountRole(currentUser?.role ?? 'user');
                setOrganizerNames(currentUser?.organizerNames ?? []);
            }
        };
        void loadAccountProfile();
        return () => { cancelled = true; };
    }, [currentUser?.uid, currentUser?.role, currentUser?.organizerNames]);

    useEffect(() => {
        let cancelled = false;
        const loadEvents = async () => {
            if (!currentUser?.uid) {
                setAllEvents([]);
                setIsLoadingLikedEvents(false);
                return;
            }
            setIsLoadingLikedEvents(true);
            try {
                const events = await getEvents();
                if (cancelled) return;
                setAllEvents(events);
            } finally {
                if (!cancelled) setIsLoadingLikedEvents(false);
            }
        };
        void loadEvents();
        return () => { cancelled = true; };
    }, [currentUser?.uid]);

    const likedEvents = useMemo(
        () => filterAndSortLikedEvents(allEvents, Array.from(likedIds)),
        [allEvents, likedIds],
    );

    async function handleFacebookConnect() {
        try {
            setFbConnecting(true);
            setFbError(null);
            window.location.href = await getFacebookAuthUrl();
        } catch (err) {
            setFbError(err instanceof Error ? err.message : 'Could not start Facebook login.');
        } finally {
            setFbConnecting(false);
        }
    }

    async function handleSignOut() {
        if (!window.confirm('Are you sure you want to log out?')) return;
        try {
            setIsSigningOut(true);
            await logout();
            navigate('/login', { replace: true });
        } finally {
            setIsSigningOut(false);
        }
    }

    return {
        currentUser,
        accountRole,
        organizerNames,
        isSigningOut,
        fbConnecting,
        fbError,
        isLoadingLikedEvents,
        likedEvents,
        userLabel: currentUser?.displayName || currentUser?.email || 'Profile',
        username: buildUsername(currentUser),
        profileImage: currentUser?.photoURL,
        handleFacebookConnect,
        handleSignOut,
    };
}
