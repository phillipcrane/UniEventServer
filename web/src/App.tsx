import { RouterProvider } from 'react-router-dom';
import { router } from './router';
import { AuthProvider } from './context/AuthContext';
import { LikesProvider } from './context/LikesContext';

function App() {
  return (
    <AuthProvider>
      <LikesProvider>
        <RouterProvider router={router} />
      </LikesProvider>
    </AuthProvider>
  );
}

export default App
