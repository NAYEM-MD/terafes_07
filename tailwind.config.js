/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        ronin: {
          bg: "#090A0F",
          dark: "#0F1117",
          card: "#141722",
          surface: "#1A1E2C",
          border: "rgba(255, 255, 255, 0.08)",
          borderGlow: "rgba(229, 57, 53, 0.25)",
          red: {
            DEFAULT: "#E53935",
            hover: "#EF5350",
            dark: "#B71C1C",
            glow: "rgba(229, 57, 53, 0.4)",
          },
          gold: {
            DEFAULT: "#D4AF37",
            light: "#F3E5AB",
            muted: "#A28325",
            glow: "rgba(212, 175, 55, 0.3)",
          },
          cyan: {
            DEFAULT: "#00E5FF",
            glow: "rgba(0, 229, 255, 0.35)",
          },
        },
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'sans-serif'],
        display: ['"Cinzel"', 'serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
        japanese: ['"Noto Serif JP"', 'serif'],
      },
      backgroundImage: {
        'samurai-gradient': 'linear-gradient(135deg, rgba(229, 57, 53, 0.15) 0%, rgba(20, 23, 34, 0.8) 50%, rgba(10, 11, 15, 0.95) 100%)',
        'gold-gradient': 'linear-gradient(135deg, #D4AF37 0%, #F3E5AB 50%, #A28325 100%)',
        'crimson-gradient': 'linear-gradient(135deg, #E53935 0%, #B71C1C 100%)',
      },
      boxShadow: {
        'red-glow': '0 0 25px -5px rgba(229, 57, 53, 0.4)',
        'gold-glow': '0 0 25px -5px rgba(212, 175, 55, 0.3)',
        'cyan-glow': '0 0 25px -5px rgba(0, 229, 255, 0.3)',
        'card-glow': '0 8px 32px 0 rgba(0, 0, 0, 0.37)',
      },
      animation: {
        'pulse-slow': 'pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'glow': 'glow 2s ease-in-out infinite alternate',
        'float': 'float 6s ease-in-out infinite',
      },
      keyframes: {
        glow: {
          '0%': { boxShadow: '0 0 10px rgba(229, 57, 53, 0.2)' },
          '100%': { boxShadow: '0 0 25px rgba(229, 57, 53, 0.6)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-8px)' },
        },
      },
    },
  },
  plugins: [],
}
