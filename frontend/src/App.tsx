import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import LoginForm from './pages/Auth/LoginForm';
import RegisterForm from './pages/Auth/RegisterForm';
import PlayPage from './pages/PlayPage';
import ProfileView from './pages/Profile/ProfileView';
import { useAuth } from './contexts/AuthContext';
import TasksView from './pages/Tasks/TasksView';
import AdminUsersView from './pages/Admin/UsersView';

const PrivateRoute: React.FC<{ children: React.ReactElement }> = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <div>Ładowanie...</div>;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return children;
};

// AdminRoute: additional guard that checks user.role === 'ADMIN'
const AdminRoute: React.FC<{ children: React.ReactElement }> = ({ children }) => {
  const { user, loading } = useAuth();
  if (loading) return <div>Ładowanie...</div>;
  if (!user || user.role !== 'ADMIN') return <Navigate to="/" replace />;
  return children;
};

const App: React.FC = () => {
  return (
    <Routes>
      <Route
        path="/tasks"
        element={
          <PrivateRoute>
            <TasksView />
          </PrivateRoute>
        }
      />
      <Route path="/login" element={<LoginForm />} />
      <Route path="/register" element={<RegisterForm />} />
      <Route
        path="/play"
        element={
          <PrivateRoute>
            <PlayPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <PrivateRoute>
            <ProfileView />
          </PrivateRoute>
        }
      />

      {/* Admin routes: protected by authentication and role check */}
      <Route
        path="/admin/users"
        element={
          <PrivateRoute>
            <AdminRoute>
              <AdminUsersView />
            </AdminRoute>
          </PrivateRoute>
        }
      />

      <Route path="/" element={<Navigate to="/play" replace />} />
    </Routes>
  );
};

export default App;
