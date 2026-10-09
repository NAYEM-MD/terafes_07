import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppProvider } from './context/AppContext';
import { AppLayout } from './components/layout/AppLayout';
import { LandingPage } from './pages/LandingPage';
import { OnboardingPage } from './pages/OnboardingPage';
import { DashboardPage } from './pages/DashboardPage';
import { DiscoverPage } from './pages/DiscoverPage';
import { OpportunityDetailPage } from './pages/OpportunityDetailPage';
import { MentorInterviewPage } from './pages/MentorInterviewPage';
import { InterviewAnalysisPage } from './pages/InterviewAnalysisPage';
import { ReportPage } from './pages/ReportPage';
import { ProfilePage } from './pages/ProfilePage';

export function App() {
  return (
    <BrowserRouter>
      <AppProvider>
        <Routes>
          {/* Landing Page without dashboard sidebar */}
          <Route path="/" element={<LandingPage />} />

          {/* Onboarding Wizard */}
          <Route path="/onboarding" element={<OnboardingPage />} />

          {/* Main App Layout with Left Sidebar, Top Bar, and Transitions */}
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/discover" element={<DiscoverPage />} />
            <Route path="/opportunity/:id" element={<OpportunityDetailPage />} />
            <Route path="/mentor" element={<MentorInterviewPage />} />
            <Route path="/analysis" element={<InterviewAnalysisPage />} />
            <Route path="/report" element={<ReportPage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Route>

          {/* Catch-all fallback */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AppProvider>
    </BrowserRouter>
  );
}

export default App;
