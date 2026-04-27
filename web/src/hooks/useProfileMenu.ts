import { useRef, useState } from 'react';
import { useClickOutside } from './useClickOutside';

export function useProfileMenu() {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  useClickOutside(menuRef, isOpen, () => setIsOpen(false));

  return { isOpen, setIsOpen, menuRef };
}
