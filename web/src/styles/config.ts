// Centralized theme tokens for use in JS/TS
export const theme = {
  colors: {
    // Brand
    dtuAccent: '#E85D3B',
    dtuAccentLight: '#F5B8A6',
    dtuAccentHover: '#D84D2B',

    // Status
    statusError: '#8b1a1a',
    statusErrorLight: '#ffb0b0',
    statusSuccess: '#1f6b3d',
    statusSuccessLight: '#9cf0c2',

    // Toggle
    toggleTrackLight: '#d0d0d0',
    toggleThumbLight: '#ffffff',
    toggleIconLight: '#1a1a1a',
    toggleTrackDark: '#000000',
    toggleThumbDark: '#111111',
    toggleIconDark: '#ffffff',

    // Auth / Cards
    authCardBgStart: 'rgba(255,255,255,0.95)',
    authCardBgEnd: 'rgba(247,239,236,0.92)',
    authCardShadow: 'rgba(53,34,27,0.16)',
    authCardInset: 'rgba(255,255,255,0.75)',

    // Misc
    textOnAccent: '#ffffff',
  },
};

export type Theme = typeof theme;
export default theme;
