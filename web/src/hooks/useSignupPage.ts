import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { signupWithEmail } from '../services/auth';
import { mapAuthError, isValidEmail } from '../utils/authUtils';
import { verifyOrganizerKey } from '../services/dal';
import type { AccountRole } from '../types';

export function useSignupPage() {
    const navigate = useNavigate();
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [organizerPasswords, setOrganizerPasswords] = useState<string[]>(['']);
    const [accountRole, setAccountRole] = useState<AccountRole | null>(null);
    const [isRoleModalOpen, setIsRoleModalOpen] = useState(true);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');

    function updateOrganizerCode(index: number, value: string) {
        setOrganizerPasswords((current) => current.map((code, i) => (i === index ? value : code)));
    }

    function addOrganizerCodeField() {
        setOrganizerPasswords((current) => [...current, '']);
    }

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
        if (password.length < 12) {
            setErrorMessage('Password must be at least 12 characters.');
            return;
        }
        if (password !== confirmPassword) {
            setErrorMessage('Passwords do not match.');
            return;
        }

        // Sync check before hitting the network: at least one code must be entered
        let enteredCodes: string[] = [];
        if (accountRole === 'organizer') {
            enteredCodes = organizerPasswords.map((c) => c.trim()).filter(Boolean);
            if (!enteredCodes.length) {
                setErrorMessage('Please enter at least one organizer access password.');
                return;
            }
        }

        try {
            setIsLoading(true);

            let confirmationToken: string | undefined;
            if (accountRole === 'organizer') {
                for (const code of enteredCodes) {
                    const verification = await verifyOrganizerKey(code);
                    if (!verification) {
                        setErrorMessage('Organizer access password is incorrect.');
                        return;
                    }
                    confirmationToken = verification.confirmationToken;
                }
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
        organizerPasswords,
        accountRole, setAccountRole,
        isRoleModalOpen, setIsRoleModalOpen,
        isLoading,
        errorMessage,
        updateOrganizerCode,
        addOrganizerCodeField,
        handleSubmit,
    };
}
