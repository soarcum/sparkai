/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          bg: '#0A0C10',        // 极深太空暗调背景色
          card: 'rgba(21, 26, 38, 0.65)',    // 玻璃卡片背景色
          border: 'rgba(255, 255, 255, 0.08)', // 极细白边框
          primary: '#8B5CF6',   // 科技紫
          secondary: '#06B6D4', // 霓虹青
          accent: '#EC4899',    // 亮粉
          text: '#E5E7EB',      // 高亮浅灰
          textMuted: '#9CA3AF'  // 暗淡灰
        }
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
      },
      boxShadow: {
        'glow-cyan': '0 0 25px rgba(6, 182, 212, 0.3)',
        'glow-purple': '0 0 25px rgba(139, 92, 246, 0.3)',
        'glass-card': '0 8px 32px 0 rgba(0, 0, 0, 0.5)',
      }
    },
  },
  plugins: [],
}
