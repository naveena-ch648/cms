import { useState, FormEvent } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate, useLocation } from 'react-router-dom';
import TwoFactorVerifyPage from './TwoFactorVerifyPage';

export default function LoginPage() {
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const initialError = params.get('error') ? decodeURIComponent(params.get('error')!) : "";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(initialError);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pendingTwoFactor, setPendingTwoFactor] = useState<{ pendingToken: string; method: 'TOTP' | 'EMAIL' } | null>(null);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleGoogleLogin = async () => {
    try {
      // We can directly navigate the browser to the backend endpoint
      window.location.href = 'http://localhost:8080/api/v1/auth/google/initiate';
    } catch (err) {
      console.error(err);
      setError("Failed to initiate Google Login");
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await login(email, password);
      navigate("/");
    } catch (err: any) {
      // HTTP 202 means 2FA is required
      if (err.response?.status === 202 && err.response?.data?.data?.requiresTwoFactor === 'true') {
        const { pendingToken, method } = err.response.data.data;
        setPendingTwoFactor({ pendingToken, method: method as 'TOTP' | 'EMAIL' });
      } else {
        setError(err.response?.data?.error?.message || 'Login failed');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const pageStyle: React.CSSProperties = {
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    minHeight: "100vh",
    background: "#f5f7fb",
    padding: 16,
  };

  const cardStyle: React.CSSProperties = {
    width: 380,
    padding: 28,
    background: "#ffffff",
    borderRadius: 8,
    boxShadow: "0 6px 18px rgba(31,41,55,0.08)",
    color: "#1f2937",
  };

  const labelStyle: React.CSSProperties = {
    display: "block",
    marginBottom: 6,
    color: "#374151",
    fontSize: 14,
  };
  const inputStyle: React.CSSProperties = {
    display: "block",
    width: "100%",
    padding: 10,
    border: "1px solid #e5e7eb",
    borderRadius: 6,
    boxSizing: "border-box",
  };
  const errorStyle: React.CSSProperties = {
    color: "#b00020",
    marginBottom: 12,
  };
  const buttonStyle: React.CSSProperties = {
    width: "100%",
    padding: 10,
    background: "#2563eb",
    color: "#fff",
    border: "none",
    borderRadius: 6,
    cursor: "pointer",
    fontWeight: 500,
    marginTop: 8,
  };
  const googleButtonStyle: React.CSSProperties = {
    width: "100%",
    padding: 10,
    background: "#fff",
    color: "#374151",
    border: "1px solid #d1d5db",
    borderRadius: 6,
    cursor: "pointer",
    fontWeight: 500,
    marginTop: 16,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
  };

  if (pendingTwoFactor) {
    return (
      <TwoFactorVerifyPage
        pendingToken={pendingTwoFactor.pendingToken}
        method={pendingTwoFactor.method}
        onCancel={() => setPendingTwoFactor(null)}
      />
    );
  }

  return (
    <div style={pageStyle}>
      <form onSubmit={handleSubmit} style={cardStyle}>
        <div style={{ textAlign: "center", marginBottom: 16 }}>
          <div style={{ fontSize: 28, fontWeight: 700, color: "#111827" }}>
            CMS
          </div>
          <div style={{ fontSize: 16, color: "#6b7280", marginTop: 4 }}>
            Sign in to your account
          </div>
        </div>
        {error && <div style={errorStyle}>{error}</div>}
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="email" style={labelStyle}>
            Email
          </label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            style={inputStyle}
          />
        </div>
        <div style={{ marginBottom: 14 }}>
          <label htmlFor="password" style={labelStyle}>
            Password
          </label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            style={inputStyle}
          />
        </div>
        <button type="submit" disabled={isSubmitting} style={buttonStyle}>
          {isSubmitting ? "Signing in..." : "Sign In"}
        </button>

        <div style={{ display: 'flex', alignItems: 'center', margin: '20px 0' }}>
          <div style={{ flex: 1, height: 1, background: '#e5e7eb' }}></div>
          <div style={{ padding: '0 10px', color: '#6b7280', fontSize: 14 }}>OR</div>
          <div style={{ flex: 1, height: 1, background: '#e5e7eb' }}></div>
        </div>

        <button type="button" onClick={handleGoogleLogin} disabled={isSubmitting} style={googleButtonStyle}>
          <svg width="18" height="18" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
            <path d="M1 1h22v22H1z" fill="none"/>
          </svg>
          Sign in with Google
        </button>
      </form>
    </div>
  );
}
