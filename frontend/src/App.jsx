import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Navbar } from './components/common/Navbar';
import { Footer } from './components/common/Footer';
import { NotificationDrawer } from './components/common/NotificationDrawer';
import { ProtectedRoute } from './components/common/ProtectedRoute';

// Public Pages
import { LandingPage } from './pages/public/LandingPage';
import { MarketplacePage } from './pages/public/MarketplacePage';
import { ApiDetailPage } from './pages/public/ApiDetailPage';
import { LoginPage } from './pages/public/LoginPage';
import { RegisterPage } from './pages/public/RegisterPage';

// Developer Pages
import { DeveloperDashboard } from './pages/developer/DeveloperDashboard';
import { SubscriptionsPage } from './pages/developer/SubscriptionsPage';
import { ApiKeysPage } from './pages/developer/ApiKeysPage';
import { ApiPlaygroundPage } from './pages/developer/ApiPlaygroundPage';
import { DeveloperUsagePage } from './pages/developer/DeveloperUsagePage';

// Owner Pages
import { OwnerDashboard } from './pages/owner/OwnerDashboard';
import { OwnerApisPage } from './pages/owner/OwnerApisPage';
import { CreateApiPage } from './pages/owner/CreateApiPage';
import { OwnerAnalyticsPage } from './pages/owner/OwnerAnalyticsPage';

// Admin Page
import { AdminDashboard } from './pages/admin/AdminDashboard';

export function App() {
  return (
    <div className="flex flex-col min-h-screen bg-slate-950 text-slate-100">
      <Navbar />
      <NotificationDrawer />

      <main className="flex-1">
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<LandingPage />} />
          <Route path="/marketplace" element={<MarketplacePage />} />
          <Route path="/marketplace/:slug" element={<ApiDetailPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Developer Routes */}
          <Route element={<ProtectedRoute allowedRoles={['DEVELOPER', 'ADMIN']} />}>
            <Route path="/dev/dashboard" element={<DeveloperDashboard />} />
            <Route path="/dev/subscriptions" element={<SubscriptionsPage />} />
            <Route path="/dev/keys" element={<ApiKeysPage />} />
            <Route path="/dev/playground" element={<ApiPlaygroundPage />} />
            <Route path="/dev/usage" element={<DeveloperUsagePage />} />
          </Route>

          {/* Owner Routes */}
          <Route element={<ProtectedRoute allowedRoles={['OWNER', 'API_OWNER', 'ADMIN']} />}>
            <Route path="/owner/dashboard" element={<OwnerDashboard />} />
            <Route path="/owner/apis" element={<OwnerApisPage />} />
            <Route path="/owner/apis/new" element={<CreateApiPage />} />
            <Route path="/owner/analytics" element={<OwnerAnalyticsPage />} />
          </Route>

          {/* Admin Routes */}
          <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      <Footer />
    </div>
  );
}

export default App;
