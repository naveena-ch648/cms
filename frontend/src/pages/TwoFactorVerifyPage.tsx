import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { twoFactorApi } from '../api/twoFactor';

interface Props {
  pendingToken: string;
  method: 'TOTP' | 'EMAIL';
  onCancel: () => void;
}

export default function TwoFactorVerifyPage({ pendingToken, method, onCancel }: Props) {
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function handleVerify(e: React.FormEvent) {
    e.preventDefault();
    if (!code.trim()) return;
    setError(null);
    setLoading(true);
    try {
      const res = await twoFactorApi.verifyLogin(pendingToken, code.trim());
      const { accessToken, refreshToken } = res.data.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      navigate('/');
    } catch {
      setError('Invalid or expired code. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={containerStyle}>
      <div style={cardStyle}>
        <h1 style={{ fontSize: 22, fontWeight: 700, marginBottom: 6 }}>Verification required</h1>
        <p style={{ color: '#5f6368', fontSize: 14, marginBottom: 24 }}>
          {method === 'EMAIL'
            ? 'A 6-digit code has been sent to your email address.'
            : 'Enter the 6-digit code from your authenticator app.'}
        </p>

        <form onSubmit={handleVerify}>
          <input
            type="text"
            inputMode="numeric"
            autoFocus
            maxLength={10}
            placeholder={method === 'TOTP' ? '6-digit code' : '6-digit code or backup code'}
            value={code}
            onChange={(e) => setCode(e.target.value)}
            style={inputStyle}
          />

          {error && <p style={{ color: '#ea4335', fontSize: 13, margin: '8px 0' }}>{error}</p>}

          <button type="submit" disabled={loading || !code.trim()} style={btnStyle}>
            {loading ? 'Verifying…' : 'Verify'}
          </button>

          <button type="button" onClick={onCancel} style={cancelStyle}>
            Back to login
          </button>
        </form>
      </div>
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  minHeight: '100vh',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: '#f4f6f8',
};

const cardStyle: React.CSSProperties = {
  background: '#fff',
  borderRadius: 12,
  padding: '40px 36px',
  width: '100%',
  maxWidth: 400,
  boxShadow: '0 2px 16px rgba(0,0,0,0.10)',
};

const inputStyle: React.CSSProperties = {
  display: 'block',
  width: '100%',
  padding: '12px 14px',
  fontSize: 18,
  border: '1px solid #dadce0',
  borderRadius: 6,
  marginBottom: 4,
  letterSpacing: 6,
  textAlign: 'center',
  boxSizing: 'border-box',
};

const btnStyle: React.CSSProperties = {
  display: 'block',
  width: '100%',
  marginTop: 12,
  padding: '12px',
  background: '#1a73e8',
  color: '#fff',
  border: 'none',
  borderRadius: 6,
  fontWeight: 600,
  fontSize: 15,
  cursor: 'pointer',
};

const cancelStyle: React.CSSProperties = {
  display: 'block',
  width: '100%',
  marginTop: 10,
  padding: '10px',
  background: 'none',
  color: '#5f6368',
  border: 'none',
  fontWeight: 500,
  fontSize: 14,
  cursor: 'pointer',
};
