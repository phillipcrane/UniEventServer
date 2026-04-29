import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginWithEmail } from '../services/auth';
import { mapAuthError, isValidEmail } from '../utils/authUtils';

export function useLoginPage() {
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setErrorMessage('');

        const trimmedEmail = email.trim();
        if (!trimmedEmail || !password) {
            setErrorMessage('Please provide both email and password.');
            return;
        }
        if (!isValidEmail(trimmedEmail)) {
            setErrorMessage('Please provide a valid email address.');
            return;
        }

        try {
            setIsLoading(true);
            await loginWithEmail(trimmedEmail, password);
            navigate('/', { replace: true });
        } catch (error) {
            setErrorMessage(mapAuthError(error));
        } finally {
            setIsLoading(false);
        }
    }

    return { email, setEmail, password, setPassword, isLoading, errorMessage, handleSubmit };
}
