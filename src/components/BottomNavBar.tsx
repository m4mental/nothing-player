import React from 'react';
import type { MainTab } from '../types/media';
import { Film, Music, Settings } from 'lucide-react';
import { triggerHaptic } from '../services/haptic';

interface BottomNavBarProps {
  activeTab: MainTab;
  setActiveTab: (tab: MainTab) => void;
  videoCount: number;
  musicCount: number;
}

export const BottomNavBar: React.FC<BottomNavBarProps> = ({
  activeTab,
  setActiveTab,
  videoCount,
  musicCount
}) => {
  const tabs: { id: MainTab; label: string; icon: React.ReactNode; badge?: number }[] = [
    { 
      id: 'VIDEOS', 
      label: 'VIDEOS', 
      icon: <Film className="w-4 h-4" />, 
      badge: videoCount 
    },
    { 
      id: 'MUSIC', 
      label: 'MUSIC', 
      icon: <Music className="w-4 h-4" />, 
      badge: musicCount 
    },
    { 
      id: 'ME', 
      label: 'SETTINGS', 
      icon: <Settings className="w-4 h-4" /> 
    },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-[#080808]/95 backdrop-blur-xl border-t border-white/10 px-4 pt-1.5 pb-safe pb-3 flex items-center justify-around">
      {tabs.map((tab) => {
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => {
              triggerHaptic('selection');
              setActiveTab(tab.id);
            }}
            className="flex-1 flex flex-col items-center justify-center py-1 transition-all relative group active-press"
          >
            {/* Active Pill Accent */}
            <div className={`flex items-center justify-center px-4 py-1 rounded-full transition-all duration-200 ${
              isActive 
                ? 'bg-white text-black font-bold shadow-lg shadow-white/10' 
                : 'text-white/50 hover:text-white/80'
            }`}>
              <div className="relative">
                {tab.icon}
                {tab.badge !== undefined && tab.badge > 0 && !isActive && (
                  <span className="absolute -top-1.5 -right-2.5 px-1.5 py-0.2 rounded-full bg-[#D71921] text-white text-[8px] font-ndot-num font-bold">
                    {tab.badge}
                  </span>
                )}
              </div>
            </div>

            <span className={`text-[10px] font-ndot mt-1 tracking-wider ${
              isActive ? 'text-white font-bold' : 'text-white/40 group-hover:text-white/60'
            }`}>
              {tab.label}
            </span>

            {/* Nothing Red Active Indicator Dot */}
            {isActive && (
              <div className="w-1 h-1 rounded-full bg-[#D71921] glow-red mt-0.5" />
            )}
          </button>
        );
      })}
    </nav>
  );
};
