import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { signupWithEmail, upgradeToOrganizer } from '../services/auth';
import { verifyOrganizerKey } from '../services/dal';
import { isValidEmail, mapAuthError } from '../utils/authUtils';
import { useAuth } from '../context/AuthContext';
import { ORGANIZER_SIGNUP_SUCCESS_REDIRECT_MS, PASSWORD_MIN_LENGTH } from '../constants';

export function useOrganizerSignupPage() {
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const [keyInput, setKeyInput] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [confirmationToken, setConfirmationToken] = useState('');
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isRegistering, setIsRegistering] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [currentStep, setCurrentStep] = useState<1 | 2>(1);
  const [showSuccessMessage, setShowSuccessMessage] = useState(false);

  async function handleVerifyKey(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage('');

    const trimmedKey = keyInput.trim();
    if (!trimmedKey) {
      setErrorMessage('Key is required.');
      return;
    }

    setIsVerifying(true);
    try {
      const result = await verifyOrganizerKey(trimmedKey);
      if (!result) {
        setErrorMessage('Organizer access key is invalid.');
        return;
      }

      setConfirmationToken(result.confirmationToken);
      setEmail(result.email);
      setCurrentStep(2);
    } catch (error) {
      setErrorMessage(mapAuthError(error));
    } finally {
      setIsVerifying(false);
    }
  }

  // Used when the user is already logged in - upgrades their existing account.
  async function handleUpgrade(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage('');
    setIsRegistering(true);
    try {
      await upgradeToOrganizer(confirmationToken);
      setShowSuccessMessage(true);
      window.setTimeout(() => navigate('/profile', { replace: true }), ORGANIZER_SIGNUP_SUCCESS_REDIRECT_MS);
    } catch (error) {
      setErrorMessage(mapAuthError(error));
    } finally {
      setIsRegistering(false);
    }
  }

  // Used when the user is not logged in - creates a new organizer account.
  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage('');

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

    setIsRegistering(true);
    try {
      await signupWithEmail({
        username: trimmedUsername,
        email: trimmedEmail,
        password,
        role: 'organizer',
        confirmationToken,
      });
      setShowSuccessMessage(true);
      window.setTimeout(() => navigate('/login', { replace: true }), ORGANIZER_SIGNUP_SUCCESS_REDIRECT_MS);
    } catch (error) {
      setErrorMessage(mapAuthError(error));
    } finally {
      setIsRegistering(false);
    }
  }

  return {
    currentUser,
    keyInput,
    setKeyInput,
    isVerifying,
    email,
    username,
    setUsername,
    password,
    setPassword,
    confirmPassword,
    setConfirmPassword,
    isRegistering,
    errorMessage,
    currentStep,
    setCurrentStep,
    showSuccessMessage,
    handleVerifyKey,
    handleUpgrade,
    handleRegister,
    navigate,
  };
}
