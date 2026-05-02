import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useLikes } from '../context/LikesContext';
import type { Event } from '../types';

type LikeButtonProps = {
  event: Event;
  compact?: boolean;
  iconOnly?: boolean;
  className?: string;
  likedLabel?: string;
  unlikedLabel?: string;
  onToggleChange?: (isLiked: boolean) => void;
};

export function LikeButton({
  event,
  compact = false,
  iconOnly = false,
  className = '',
  likedLabel = 'Liked',
  unlikedLabel = 'Like',
  onToggleChange,
}: LikeButtonProps) {
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const { isLiked, toggle } = useLikes();
  const [isUpdating, setIsUpdating] = useState(false);
  const liked = currentUser?.uid ? isLiked(event.id) : false;

  const handleClick = async (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    e.stopPropagation();

    if (isUpdating) {
      return;
    }

    if (!currentUser?.uid) {
      navigate('/login');
      return;
    }

    try {
      setIsUpdating(true);
      const nextLiked = await toggle(event.id);
      onToggleChange?.(nextLiked);
    } finally {
      setIsUpdating(false);
    }
  };

  const label = liked ? likedLabel : unlikedLabel;
  const buttonClasses = compact
    ? 'inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-semibold transition-colors duration-200'
    : 'inline-flex items-center gap-2 rounded-lg border px-4 py-2 text-sm font-semibold transition-colors duration-200';
  const iconOnlyClasses = iconOnly
    ? 'h-12 w-12 justify-center rounded-xl border p-0 shadow-[0_8px_18px_var(--like-button-icon-shadow)] backdrop-blur-md transition-colors duration-200'
    : '';
  const stateClasses = liked
    ? iconOnly
      ? 'border-[var(--liked-button-border)] bg-[var(--liked-button-icon-bg)] text-[var(--liked-button-text)] hover:bg-[var(--liked-button-icon-bg-hover)]'
      : 'border-[var(--liked-button-border)] bg-[var(--liked-button-bg)] text-[var(--liked-button-text)] hover:bg-[var(--liked-button-bg-hover)]'
    : iconOnly
      ? 'border-[var(--panel-border)] bg-[var(--liked-button-icon-bg)] text-[var(--text-primary)] hover:bg-[var(--liked-button-icon-bg-hover)]'
      : 'border-[var(--panel-border)] bg-[var(--panel-bg)] text-[var(--text-primary)] hover:bg-[var(--button-hover)]';

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={isUpdating}
      aria-pressed={liked}
      aria-label={label}
      className={[buttonClasses, iconOnlyClasses, stateClasses, className].filter(Boolean).join(' ')}
    >
      <Heart
        size={iconOnly ? 24 : 18}
        strokeWidth={iconOnly ? 2.6 : 2.2}
        className={liked ? 'text-[var(--like-icon-active)]' : undefined}
        fill={liked ? 'currentColor' : 'none'}
      />
      {!iconOnly && label}
    </button>
  );
}