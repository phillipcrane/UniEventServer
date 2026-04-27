/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react';
import { useAuth } from './AuthContext';
import { getLikedEventIdsAsync, toggleLikedEvent } from '../services/likes';

// A context is a REACT-particular way to share state across various REACT components without having to
// manually pass down variables through components. It obviously makes sense to do this for authentication,
// but also here in the case of likes

type LikesContextValue = {
  likedIds: Set<string>;
  isLiked: (eventId: string) => boolean;
  toggle: (eventId: string) => Promise<boolean>;
};

const LikesContext = createContext<LikesContextValue>({
  likedIds: new Set(),
  isLiked: () => false,
  toggle: async () => false,
});

export function LikesProvider({ children }: { children: ReactNode }) {
  const { currentUser } = useAuth();
  const uid = currentUser?.uid ?? null;
  const [likedIds, setLikedIds] = useState<Set<string>>(new Set());

  const reload = useCallback(async () => {
    const ids = await getLikedEventIdsAsync(uid);
    setLikedIds(new Set(ids));
  }, [uid]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const toggle = useCallback(async (eventId: string): Promise<boolean> => {
    if (!uid) return false;
    const next = await toggleLikedEvent(uid, eventId);
    setLikedIds(prev => {
      const updated = new Set(prev);
      if (next) updated.add(eventId);
      else updated.delete(eventId);
      return updated;
    });
    return next;
  }, [uid]);

  const isLiked = useCallback((eventId: string) => likedIds.has(eventId), [likedIds]);

  return (
    <LikesContext.Provider value={{ likedIds, isLiked, toggle }}>
      {children}
    </LikesContext.Provider>
  );
}

export function useLikes(): LikesContextValue {
  return useContext(LikesContext);
}
