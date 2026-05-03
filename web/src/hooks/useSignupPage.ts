import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { signupWithEmail } from '../services/auth';
import { mapAuthError, isValidEmail } from '../utils/authUtils';
import { verifyOrganizerKey } from '../services/dal';
import { PASSWORD_MIN_LENGTH } from '../constants';
import type { AccountRole } from '../types';

export function useSignupPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [organizerKey, setOrganizerKey] = useState('');
  const [accountRole, setAccountRole] = useState<AccountRole | null>(null);
  const [isRoleModalOpen, setIsRoleModalOpen] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage('');

    if (!accountRole) {
      setIsRoleModalOpen(true);
      return;
    }

    const trimmedUsername = username.trim();
    const trimmedEmail = email.trim();

    if (!trimmedUsername || !trimmedEmail || !password || !confirmPassword) {
      setErrorMessage('Please fill in all fields.');
      return;
    }
    if (!isValidEmail(trimmedEmail)) {
      setErrorMessage('Please provide a valid email address.');
      return;
    }
    if (password.length < PASSWORD_MIN_LENGTH) {
      setErrorMessage(`Password must be at least ${PASSWORD_MIN_LENGTH} characters.`);
      return;
    }
    if (password !== confirmPassword) {
      setErrorMessage('Passwords do not match.');
      return;
    }

    const trimmedKey = organizerKey.trim();
    if (accountRole === 'organizer' && !trimmedKey) {
      setErrorMessage('Please enter your organizer invitation key.');
      return;
    }

    try {
      setIsLoading(true);

      let confirmationToken: string | undefined;
      if (accountRole === 'organizer') {
        const verification = await verifyOrganizerKey(trimmedKey);
        if (!verification) {
          setErrorMessage('Organizer invitation key is invalid.');
          return;
        }
        confirmationToken = verification.confirmationToken;
      }

      await signupWithEmail({ username: trimmedUsername, email: trimmedEmail, password, role: accountRole, confirmationToken });
      navigate('/', { replace: true });
    } catch (error) {
      setErrorMessage(mapAuthError(error));
    } finally {
      setIsLoading(false);
    }
  }

  return {
    username, setUsername,
    email, setEmail,
    password, setPassword,
    confirmPassword, setConfirmPassword,
    organizerKey, setOrganizerKey,
    accountRole, setAccountRole,
    isRoleModalOpen, setIsRoleModalOpen,
    isLoading,
    errorMessage,
    handleSubmit,
  };
}
