import React from 'react';

interface AuthPageProps {
  onLoginSuccess: (token: string, username: string) => void;
}

export default function AuthPage({ onLoginSuccess }: AuthPageProps) {
  React.useEffect(() => {
    // Parse token and username from the hash fragment if present
    const hash = window.location.hash;
    if (hash && (hash.startsWith('#token=') || hash.includes('token='))) {
      const hashStr = hash.startsWith('#') ? hash.substring(1) : hash;
      const params = new URLSearchParams(hashStr);
      const token = params.get('token');
      const username = params.get('username');
      if (token) {
        onLoginSuccess(token, username || 'Developer');
        // Clean URL fragment
        window.history.replaceState(null, '', window.location.pathname + window.location.search);
      }
    }
  }, [onLoginSuccess]);

  const handleLogin = (provider: 'google' | 'github') => {
    // Redirect to Spring Boot OAuth2 endpoint
    const javaBackendUrl = import.meta.env.VITE_JAVA_BACKEND_URL || 'http://localhost:8080';
    window.location.href = `${javaBackendUrl}/oauth2/authorization/${provider}`;
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'radial-gradient(circle at 10% 20%, rgb(0, 0, 0) 0%, rgb(8, 11, 22) 90%)',
      fontFamily: 'Outfit, Inter, system-ui, sans-serif',
      color: '#fff',
      padding: '24px',
      overflow: 'hidden',
      position: 'relative'
    }}>
      {/* Background blobs for premium glassmorphic vibe */}
      <div style={{
        position: 'absolute',
        top: '-10%',
        left: '-10%',
        width: '50vw',
        height: '50vw',
        background: 'radial-gradient(circle, rgba(249, 115, 22, 0.15) 0%, rgba(0, 0, 0, 0) 70%)',
        zIndex: 0,
        pointerEvents: 'none'
      }} />
      <div style={{
        position: 'absolute',
        bottom: '-10%',
        right: '-10%',
        width: '60vw',
        height: '60vw',
        background: 'radial-gradient(circle, rgba(14, 165, 233, 0.12) 0%, rgba(0, 0, 0, 0) 70%)',
        zIndex: 0,
        pointerEvents: 'none'
      }} />

      {/* Login Card */}
      <div style={{
        width: '100%',
        maxWidth: '440px',
        padding: '48px 40px',
        borderRadius: '24px',
        background: 'rgba(255, 255, 255, 0.03)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        backdropFilter: 'blur(16px)',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
        textAlign: 'center',
        zIndex: 1,
        transition: 'all 0.3s ease'
      }}>
        {/* Logo/Icon */}
        <div style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '64px',
          height: '64px',
          borderRadius: '16px',
          background: 'linear-gradient(135deg, #f97316 0%, #ff5722 100%)',
          fontSize: '32px',
          marginBottom: '24px',
          boxShadow: '0 0 30px rgba(249, 115, 22, 0.3)'
        }}>
          🕸
        </div>

        <h1 style={{
          fontSize: '28px',
          fontWeight: 800,
          letterSpacing: '-0.5px',
          margin: '0 0 8px 0',
          background: 'linear-gradient(to right, #ffffff, #94a3b8)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent'
        }}>
          Welcome to CodeMap AI
        </h1>
        <p style={{
          fontSize: '14px',
          color: '#94a3b8',
          lineHeight: '1.6',
          margin: '0 0 32px 0'
        }}>
          Visualize codebases, trace dependencies, and query repositories. Please sign in to establish a secure, private session.
        </p>

        {/* Buttons wrapper */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {/* Google Button */}
          <button
            onClick={() => handleLogin('google')}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '12px',
              width: '100%',
              padding: '14px',
              borderRadius: '12px',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              background: 'rgba(255, 255, 255, 0.05)',
              color: '#fff',
              fontSize: '15px',
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'all 0.2s ease',
              outline: 'none'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = 'rgba(255, 255, 255, 0.1)';
              e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.2)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'rgba(255, 255, 255, 0.05)';
              e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.1)';
            }}
          >
            {/* SVG Google Logo */}
            <svg width="18" height="18" viewBox="0 0 18 18">
              <path fill="#4285F4" d="M17.64 9.2c0-.63-.06-1.25-.16-1.84H9v3.47h4.84c-.21 1.12-.84 2.07-1.79 2.7v2.24h2.9c1.7-1.57 2.69-3.88 2.69-6.57z"/>
              <path fill="#34A853" d="M9 18c2.43 0 4.47-.8 5.96-2.23l-2.91-2.24c-.8.54-1.84.87-3.05.87-2.34 0-4.33-1.58-5.03-3.7H.95v2.3C2.43 15.89 5.5 18 9 18z"/>
              <path fill="#FBBC05" d="M3.97 10.7c-.18-.54-.28-1.12-.28-1.7s.1-1.16.28-1.7V5H.95C.35 6.2 0 7.57 0 9s.35 2.8 1 4l2.97-2.3z"/>
              <path fill="#EA4335" d="M9 3.58c1.32 0 2.5.45 3.44 1.35L15 2.02C13.46.59 11.43 0 9 0 5.5 0 2.43 2.11.95 5.09L3.97 7.4c.7-2.12 2.69-3.82 5.03-3.82z"/>
            </svg>
            Continue with Google
          </button>


        </div>

        {/* Footer info */}
        <div style={{
          marginTop: '32px',
          fontSize: '11px',
          color: '#64748b',
          display: 'flex',
          justifyContent: 'center',
          gap: '16px'
        }}>
          <span>🔒 Secure Session</span>
          <span>⚡ Local Execution</span>
        </div>
      </div>
    </div>
  );
}
