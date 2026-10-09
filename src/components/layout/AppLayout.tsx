import React, { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { ToastContainer } from '../common/ToastContainer';
import { motion, AnimatePresence } from 'framer-motion';

export const AppLayout: React.FC = () => {
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);
  const location = useLocation();

  // If on landing page or onboarding, we may render full screen or tailored container
  const isLanding = location.pathname === '/';

  if (isLanding) {
    return (
      <div className="min-h-screen bg-[#090A0F] text-gray-100 flex flex-col relative overflow-x-hidden">
        <ToastContainer />
        <Outlet />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#090A0F] text-gray-100 flex">
      {/* Sidebar (Desktop fixed 72rem / Mobile slide-out) */}
      <Sidebar
        isOpen={isMobileSidebarOpen}
        onCloseMobile={() => setIsMobileSidebarOpen(false)}
      />

      {/* Main Container Area */}
      <div className="flex-1 flex flex-col min-w-0 lg:pl-72 transition-all duration-300">
        <TopBar onToggleMobileSidebar={() => setIsMobileSidebarOpen(!isMobileSidebarOpen)} />

        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto relative">
          {/* Cyberpunk grid ambient background */}
          <div className="fixed inset-0 lg:left-72 cyber-grid pointer-events-none opacity-40 -z-10" />

          {/* Smooth page transition animation */}
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.25, ease: 'easeOut' }}
              className="w-full"
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </main>
      </div>

      <ToastContainer />
    </div>
  );
};
